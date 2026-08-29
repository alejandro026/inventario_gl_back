package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.CotizacionDTO;
import com.guerrero.Inventario.dto.DetalleCotizacionDTO;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.*;
import com.guerrero.Inventario.repository.*;
import com.guerrero.Inventario.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CotizacionServiceTest {

    @Mock
    private CotizacionRepository repository;
    @Mock
    private ProductoService productoService;
    @Mock
    private SucursalService sucursalService;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PromocionRepository promocionRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CotizacionService service;

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
        producto.setNombre("Libreta");
        producto.setPrecio(new BigDecimal("50.00"));
        producto.setActivo(true);
    }

    private CotizacionDTO cotizacionDtoConDetalle(int cantidad) {
        CotizacionDTO dto = new CotizacionDTO();
        dto.setIdSucursal(1L);
        DetalleCotizacionDTO detalle = new DetalleCotizacionDTO();
        detalle.setProductoId(20L);
        detalle.setCantProd(cantidad);
        dto.setDetalle(new ArrayList<>(List.of(detalle)));
        return dto;
    }

    @Test
    void guardar_nuevaCotizacion_sinPromociones_descuentoCero() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        when(sucursalService.buscar(1L)).thenReturn(sucursal);
        when(productoService.buscar(20L)).thenReturn(producto);
        when(promocionRepository.findActivePromotions(any())).thenReturn(List.of());
        when(repository.save(any(Cotizacion.class))).thenAnswer(inv -> {
            Cotizacion c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        CotizacionDTO dto = cotizacionDtoConDetalle(2); // 100.00 total
        CotizacionDTO resultado = service.guardar(dto);

        assertThat(resultado.getId()).isEqualTo(100L);
        assertThat(resultado.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(resultado.getDescuento()).isEqualByComparingTo("0.00");
        assertThat(resultado.getTotal()).isEqualByComparingTo("100.00");
        assertThat(resultado.getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    void guardar_nuevaCotizacion_conPromocionGlobal_aplicaDescuento() {
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);
        when(sucursalService.buscar(1L)).thenReturn(sucursal);
        when(productoService.buscar(20L)).thenReturn(producto);

        Promocion promo = Promocion.builder()
                .id(1L)
                .nombre("Promo 10%")
                .compraMinima(new BigDecimal("100.00"))
                .porcentajeDescuento(new BigDecimal("10.00"))
                .activa(true)
                .build();
        when(promocionRepository.findActivePromotions(any())).thenReturn(List.of(promo));

        when(repository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        CotizacionDTO dto = cotizacionDtoConDetalle(3); // 150.00 total
        CotizacionDTO resultado = service.guardar(dto);

        assertThat(resultado.getSubtotal()).isEqualByComparingTo("150.00");
        assertThat(resultado.getDescuento()).isEqualByComparingTo("15.00");
        assertThat(resultado.getTotal()).isEqualByComparingTo("135.00");
    }

    @Test
    void registrarVentaLink_cotizacionPendiente_cambiaEstadoAVendida() {
        Cotizacion cot = Cotizacion.builder()
                .id(10L)
                .estado(Cotizacion.EstadoCotizacion.PENDIENTE)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(cot));
        when(repository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarVentaLink(10L, 200L);

        assertThat(cot.getEstado()).isEqualTo(Cotizacion.EstadoCotizacion.VENDIDA);
        assertThat(cot.getVentaId()).isEqualTo(200L);
    }

    @Test
    void registrarVentaLink_cotizacionYaVendida_lanzaBusinessException() {
        Cotizacion cot = Cotizacion.builder()
                .id(10L)
                .estado(Cotizacion.EstadoCotizacion.VENDIDA)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(cot));

        assertThatThrownBy(() -> service.registrarVentaLink(10L, 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("La cotización no se encuentra PENDIENTE.");
    }

    @Test
    void eliminar_cotizacionPendiente_exitoso() {
        Cotizacion cot = Cotizacion.builder()
                .id(10L)
                .estado(Cotizacion.EstadoCotizacion.PENDIENTE)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(cot));

        service.eliminar(10L);

        verify(repository).delete(cot);
    }

    @Test
    void eliminar_cotizacionVendida_lanzaBusinessException() {
        Cotizacion cot = Cotizacion.builder()
                .id(10L)
                .estado(Cotizacion.EstadoCotizacion.VENDIDA)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(cot));

        assertThatThrownBy(() -> service.eliminar(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede eliminar una cotización que ya ha sido convertida en venta.");
    }
}
