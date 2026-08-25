package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.DetalleVentaDTO;
import com.guerrero.Inventario.dto.VentaDTO;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.InsufficientStockException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.*;
import com.guerrero.Inventario.repository.CajaTurnoRepository;
import com.guerrero.Inventario.repository.ClienteRepository;
import com.guerrero.Inventario.repository.CuentaPorCobrarRepository;
import com.guerrero.Inventario.repository.VentaRepository;
import com.guerrero.Inventario.repository.PromocionRepository;
import com.guerrero.Inventario.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private ProductoService productoService;
    @Mock
    private SucursalService sucursalService;
    @Mock
    private KardexService kardexService;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaPorCobrarRepository cuentaPorCobrarRepository;
    @Mock
    private CajaTurnoRepository cajaTurnoRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private PromocionRepository promocionRepository;

    @InjectMocks
    private VentaService service;

    private Sucursal sucursal;
    private Usuario usuario;
    private Producto producto;

    @BeforeEach
    void setUp() {
        sucursal = new Sucursal();
        sucursal.setId(1L);
        sucursal.setNombre("Centro");

        usuario = new Usuario();
        usuario.setId(9L);
        usuario.setUsername("empleado1");
        usuario.setSucursal(sucursal);

        producto = new Producto();
        producto.setId(20L);
        producto.setNombre("Cuaderno");
        producto.setPrecio(new BigDecimal("50.00"));
        producto.setPrecioCompra(new BigDecimal("30.00"));
        producto.setCantidad(10);
        producto.setActivo(true);
        producto.setControlaStock(true);
    }

    private VentaDTO ventaDtoConDetalle(int cantidad) {
        VentaDTO dto = new VentaDTO();
        dto.setIdSucursal(1L);
        DetalleVentaDTO detalle = new DetalleVentaDTO();
        detalle.setProductoId(20L);
        detalle.setCantProd(cantidad);
        dto.setDetalle(new ArrayList<>(List.of(detalle)));
        return dto;
    }

    private void mockTurnoAbierto() {
        CajaTurno turno = new CajaTurno();
        turno.setId(500L);
        when(cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(1L, 9L, "ABIERTO"))
                .thenReturn(Optional.of(turno));
    }

    private void mockGuardarVenta() {
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            if (v.getId() == null) v.setId(100L);
            return v;
        });
    }

    @Test
    void registrar_efectivo_pagoExacto_calculaTotalYSinCambio() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        when(productoService.buscar(20L)).thenReturn(producto);
        mockGuardarVenta();

        VentaDTO dto = ventaDtoConDetalle(2);
        dto.setMetodoPago("EFECTIVO");

        VentaDTO resultado = service.registrar(dto);

        assertThat(resultado.getTotal()).isEqualByComparingTo("100.00");
        assertThat(resultado.getPagoCon()).isEqualByComparingTo("100.00");
        assertThat(resultado.getCambio()).isEqualByComparingTo("0.00");
        assertThat(producto.getCantidad()).isEqualTo(8);
        verify(kardexService).registrarMovimiento(eq(producto), eq(sucursal), eq("SALIDA"), eq(2), eq("VENTA"), eq(usuario), eq(100L));
    }

    @Test
    void registrar_efectivo_conPagoMayor_calculaCambio() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        when(productoService.buscar(20L)).thenReturn(producto);
        mockGuardarVenta();

        VentaDTO dto = ventaDtoConDetalle(1);
        dto.setPagoCon(new BigDecimal("100.00"));

        VentaDTO resultado = service.registrar(dto);

        assertThat(resultado.getTotal()).isEqualByComparingTo("50.00");
        assertThat(resultado.getPagoCon()).isEqualByComparingTo("100.00");
        assertThat(resultado.getCambio()).isEqualByComparingTo("50.00");
    }

    @Test
    void registrar_credito_exitoso_generaCuentaPorCobrar() {
        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setSaldoPendiente(BigDecimal.ZERO);
        cliente.setLimiteCredito(new BigDecimal("1000.00"));

        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(productoService.buscar(20L)).thenReturn(producto);
        mockGuardarVenta();

        VentaDTO dto = ventaDtoConDetalle(2);
        dto.setMetodoPago("CREDITO");
        dto.setIdCliente(3L);

        VentaDTO resultado = service.registrar(dto);

        assertThat(resultado.getTotal()).isEqualByComparingTo("100.00");
        assertThat(cliente.getSaldoPendiente()).isEqualByComparingTo("100.00");
        verify(cuentaPorCobrarRepository).save(any(CuentaPorCobrar.class));
        verify(clienteRepository).save(cliente);
    }

    @Test
    void registrar_credito_sinCliente_lanzaBusinessException() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();

        VentaDTO dto = ventaDtoConDetalle(1);
        dto.setMetodoPago("CREDITO");

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrar_credito_limiteExcedido_lanzaBusinessException() {
        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setSaldoPendiente(new BigDecimal("980.00"));
        cliente.setLimiteCredito(new BigDecimal("1000.00"));

        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(productoService.buscar(20L)).thenReturn(producto);

        VentaDTO dto = ventaDtoConDetalle(1); // total = 50, saldo 980 + 50 = 1030 > 1000
        dto.setMetodoPago("CREDITO");
        dto.setIdCliente(3L);

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(BusinessException.class);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void registrar_sinSucursalResoluble_lanzaBusinessException() {
        when(currentUserProvider.obtenerONull()).thenReturn(null);

        VentaDTO dto = new VentaDTO();
        dto.setDetalle(new ArrayList<>());

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrar_usuarioSinTurnoAbierto_lanzaBusinessException() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        when(cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(1L, 9L, "ABIERTO"))
                .thenReturn(Optional.empty());

        VentaDTO dto = ventaDtoConDetalle(1);

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrar_sinDetalles_lanzaBusinessException() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();

        VentaDTO dto = new VentaDTO();
        dto.setIdSucursal(1L);
        dto.setDetalle(new ArrayList<>());

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrar_productoInactivo_lanzaBusinessException() {
        producto.setActivo(false);
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        when(productoService.buscar(20L)).thenReturn(producto);

        VentaDTO dto = ventaDtoConDetalle(1);

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrar_stockInsuficiente_lanzaInsufficientStockException() {
        producto.setCantidad(1);
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        when(productoService.buscar(20L)).thenReturn(producto);

        VentaDTO dto = ventaDtoConDetalle(5);

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void registrar_metodoPagoInvalido_lanzaBusinessException() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();

        VentaDTO dto = ventaDtoConDetalle(1);
        dto.setMetodoPago("BITCOIN");

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrar_estadoInvalido_lanzaBusinessException() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();

        VentaDTO dto = ventaDtoConDetalle(1);
        dto.setEstado("INVENTADO");

        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelar_ventaCompletada_reintegraStockYRegistraKardex() {
        Venta venta = new Venta();
        venta.setId(200L);
        venta.setEstado(Venta.EstadoVenta.COMPLETADA);
        venta.setMetodoPago(Venta.MetodoPago.EFECTIVO);
        venta.setSucursal(sucursal);
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantProd(3);
        venta.setDetalle(new ArrayList<>(List.of(detalle)));

        when(ventaRepository.findById(200L)).thenReturn(Optional.of(venta));
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockGuardarVenta();

        VentaDTO resultado = service.cancelar(200L);

        assertThat(resultado.getEstado()).isEqualTo("CANCELADA");
        assertThat(producto.getCantidad()).isEqualTo(13);
        verify(kardexService).registrarMovimiento(eq(producto), eq(sucursal), eq("ENTRADA"), eq(3), eq("CANCELACION"), eq(usuario), eq(200L));
    }

    @Test
    void cancelar_ventaYaCancelada_lanzaBusinessException() {
        Venta venta = new Venta();
        venta.setId(201L);
        venta.setEstado(Venta.EstadoVenta.CANCELADA);
        when(ventaRepository.findById(201L)).thenReturn(Optional.of(venta));

        assertThatThrownBy(() -> service.cancelar(201L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelar_ventaCredito_revierteSaldoClienteYCuenta() {
        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setSaldoPendiente(new BigDecimal("100.00"));

        Venta venta = new Venta();
        venta.setId(202L);
        venta.setEstado(Venta.EstadoVenta.COMPLETADA);
        venta.setMetodoPago(Venta.MetodoPago.CREDITO);
        venta.setSucursal(sucursal);
        venta.setCliente(cliente);
        venta.setDetalle(new ArrayList<>());

        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(9L);
        cxc.setVenta(venta);
        cxc.setSaldoPendiente(new BigDecimal("100.00"));
        cxc.setEstado("PENDIENTE");

        when(ventaRepository.findById(202L)).thenReturn(Optional.of(venta));
        when(cuentaPorCobrarRepository.findByClienteId(3L)).thenReturn(List.of(cxc));
        mockGuardarVenta();

        service.cancelar(202L);

        assertThat(cliente.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(cxc.getEstado()).isEqualTo("CANCELADA");
        verify(clienteRepository).save(cliente);
        verify(cuentaPorCobrarRepository).save(cxc);
    }

    @Test
    void obtener_existente_devuelveDto() {
        Venta venta = new Venta();
        venta.setId(300L);
        venta.setEstado(Venta.EstadoVenta.COMPLETADA);
        venta.setDetalle(new ArrayList<>());
        when(ventaRepository.findById(300L)).thenReturn(Optional.of(venta));

        VentaDTO dto = service.obtener(300L);

        assertThat(dto.getId()).isEqualTo(300L);
    }

    @Test
    void buscar_noExistente_lanzaResourceNotFound() {
        when(ventaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void totalVendido_rangoInvalido_lanzaBusinessException() {
        var fin = java.time.LocalDateTime.now();
        var inicio = fin.plusDays(1);

        assertThatThrownBy(() -> service.totalVendido(inicio, fin)).isInstanceOf(BusinessException.class);
    }

    @Test
    void totalVendido_sinVentas_devuelveCero() {
        var inicio = java.time.LocalDateTime.now().minusDays(1);
        var fin = java.time.LocalDateTime.now();
        when(ventaRepository.totalVendidoEntre(inicio, fin)).thenReturn(null);

        BigDecimal total = service.totalVendido(inicio, fin);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void totalVendido_conVentas_devuelveTotal() {
        var inicio = java.time.LocalDateTime.now().minusDays(1);
        var fin = java.time.LocalDateTime.now();
        when(ventaRepository.totalVendidoEntre(inicio, fin)).thenReturn(new BigDecimal("500.00"));

        BigDecimal total = service.totalVendido(inicio, fin);

        assertThat(total).isEqualByComparingTo("500.00");
    }

    @Test
    void registrar_conPromocionGlobal_aplicaDescuento() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        when(productoService.buscar(20L)).thenReturn(producto);
        mockGuardarVenta();

        VentaDTO dto = ventaDtoConDetalle(3); // 3 items * 50.00 = 150.00 subtotal
        dto.setMetodoPago("EFECTIVO");
        
        Promocion promo = Promocion.builder()
                .id(1L)
                .nombre("Promo 10%")
                .compraMinima(new BigDecimal("100.00"))
                .porcentajeDescuento(new BigDecimal("10.00"))
                .activa(true)
                .build();
                
        when(promocionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        VentaDTO resultado = service.registrar(dto);

        assertThat(resultado.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(resultado.getDescuento()).isEqualByComparingTo("15.00");
        assertThat(resultado.getTotal()).isEqualByComparingTo("135.00");
    }

    @Test
    void registrar_conPromocionDeCategoria_aplicaDescuentoElegible() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        mockGuardarVenta();

        Categoria categoria = new Categoria();
        categoria.setId(5L);
        categoria.setNombre("Papeleria");
        producto.setCategoria(categoria);
        when(productoService.buscar(20L)).thenReturn(producto);
        
        VentaDTO dto = ventaDtoConDetalle(3); // 150.00 subtotal
        dto.setMetodoPago("EFECTIVO");
        
        Promocion promo = Promocion.builder()
                .id(1L)
                .nombre("Promo Papeleria 10%")
                .compraMinima(new BigDecimal("100.00"))
                .porcentajeDescuento(new BigDecimal("10.00"))
                .activa(true)
                .categorias(java.util.Set.of(categoria))
                .build();
                
        when(promocionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        VentaDTO resultado = service.registrar(dto);

        assertThat(resultado.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(resultado.getDescuento()).isEqualByComparingTo("15.00");
        assertThat(resultado.getTotal()).isEqualByComparingTo("135.00");
    }

    @Test
    void registrar_conPromocionDeCategoria_noElegible_noAplicaDescuento() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        mockTurnoAbierto();
        mockGuardarVenta();

        Categoria categoriaProducto = new Categoria();
        categoriaProducto.setId(8L);
        producto.setCategoria(categoriaProducto);
        when(productoService.buscar(20L)).thenReturn(producto);
        
        Categoria categoriaPromo = new Categoria();
        categoriaPromo.setId(5L);
        
        VentaDTO dto = ventaDtoConDetalle(3); // 150.00 subtotal
        dto.setMetodoPago("EFECTIVO");
        
        Promocion promo = Promocion.builder()
                .id(1L)
                .nombre("Promo Papeleria 10%")
                .compraMinima(new BigDecimal("100.00"))
                .porcentajeDescuento(new BigDecimal("10.00"))
                .activa(true)
                .categorias(java.util.Set.of(categoriaPromo))
                .build();
                
        when(promocionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        VentaDTO resultado = service.registrar(dto);

        assertThat(resultado.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(resultado.getDescuento()).isEqualByComparingTo("0.00");
        assertThat(resultado.getTotal()).isEqualByComparingTo("150.00");
    }
}
