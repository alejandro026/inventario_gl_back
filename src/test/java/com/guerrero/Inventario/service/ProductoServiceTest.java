package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.ProductoDTO;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.Categoria;
import com.guerrero.Inventario.model.Producto;
import com.guerrero.Inventario.model.Sucursal;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.CategoriaRepository;
import com.guerrero.Inventario.repository.IProductoRepository;
import com.guerrero.Inventario.repository.SucursalRepository;
import com.guerrero.Inventario.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private IProductoRepository productoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private KardexService kardexService;
    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ProductoService service;

    private Categoria categoria;
    private Producto producto;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("PAPELERIA");

        producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Cuaderno");
        producto.setCodigo("COD-1");
        producto.setPrecio(new BigDecimal("45.50"));
        producto.setPrecioCompra(new BigDecimal("25.00"));
        producto.setCantidad(100);
        producto.setStockMinimo(5);
        producto.setActivo(true);
        producto.setControlaStock(true);
        producto.setCategoria(categoria);
    }

    private ProductoDTO dtoValido() {
        ProductoDTO dto = new ProductoDTO();
        dto.setNombre("Cuaderno");
        dto.setCodigo("COD-1");
        dto.setPrecio(new BigDecimal("45.50"));
        dto.setPrecioCompra(new BigDecimal("25.00"));
        dto.setCantidad(100);
        dto.setStockMinimo(5);
        dto.setActivo(true);
        dto.setControlaStock(true);
        dto.setCategoriaId(1L);
        return dto;
    }

    @Test
    void obtener_existente_devuelveDto() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        ProductoDTO dto = service.obtener(10L);

        assertThat(dto.getNombre()).isEqualTo("Cuaderno");
        assertThat(dto.getPrecio()).isEqualByComparingTo("45.50");
    }

    @Test
    void buscar_noExistente_lanzaResourceNotFound() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void productosBajoStock_mapeaLista() {
        when(productoRepository.findProductosBajoStock()).thenReturn(List.of(producto));

        List<ProductoDTO> resultado = service.productosBajoStock();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(10L);
    }

    @Test
    void crear_exitoso_guardaProducto() {
        ProductoDTO dto = dtoValido();
        when(productoRepository.existsByNombreIgnoreCase("Cuaderno")).thenReturn(false);
        when(productoRepository.existsByCodigoIgnoreCase("COD-1")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO creado = service.crear(dto);

        assertThat(creado.getNombre()).isEqualTo("Cuaderno");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    void crear_nombreDuplicado_lanzaExcepcion() {
        ProductoDTO dto = dtoValido();
        when(productoRepository.existsByNombreIgnoreCase("Cuaderno")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(dto))
                .isInstanceOf(DuplicateResourceException.class);
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crear_codigoDuplicado_lanzaExcepcion() {
        ProductoDTO dto = dtoValido();
        when(productoRepository.existsByNombreIgnoreCase("Cuaderno")).thenReturn(false);
        when(productoRepository.existsByCodigoIgnoreCase("COD-1")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(dto))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void crear_categoriaInexistente_lanzaResourceNotFound() {
        ProductoDTO dto = dtoValido();
        when(productoRepository.existsByNombreIgnoreCase("Cuaderno")).thenReturn(false);
        when(productoRepository.existsByCodigoIgnoreCase("COD-1")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_exitoso_guardaCambios() {
        ProductoDTO dto = dtoValido();
        dto.setNombre("Cuaderno Pro");
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoRepository.existsByNombreIgnoreCase("Cuaderno Pro")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductoDTO actualizado = service.actualizar(10L, dto);

        assertThat(actualizado.getNombre()).isEqualTo("Cuaderno Pro");
    }

    @Test
    void actualizar_nombreDuplicadoEnOtroProducto_lanzaExcepcion() {
        ProductoDTO dto = dtoValido();
        dto.setNombre("Otro Nombre");
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoRepository.existsByNombreIgnoreCase("Otro Nombre")).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(10L, dto))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void actualizar_sinCodigo_limpiaCodigoExistente() {
        ProductoDTO dto = dtoValido();
        dto.setCodigo(null);
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(10L, dto);

        assertThat(producto.getCodigo()).isNull();
    }

    @Test
    void ajustarStock_deltaCero_lanzaBusinessException() {
        assertThatThrownBy(() -> service.ajustarStock(10L, 0))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ajustarStock_resultadoNegativo_lanzaBusinessException() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.ajustarStock(10L, -500))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ajustarStock_incrementaYRegistraKardex() {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(2L);
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setSucursal(sucursal);

        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserProvider.obtenerONull()).thenReturn(usuario);

        ProductoDTO resultado = service.ajustarStock(10L, 20);

        assertThat(resultado.getCantidad()).isEqualTo(120);
        verify(kardexService).registrarMovimiento(eq(producto), eq(sucursal), eq("ENTRADA"), eq(20), eq("AJUSTE_MANUAL"), eq(usuario), isNull());
    }

    @Test
    void ajustarStock_sinUsuarioActual_usaPrimeraSucursalDisponible() {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(3L);

        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserProvider.obtenerONull()).thenReturn(null);
        when(sucursalRepository.findAll()).thenReturn(List.of(sucursal));

        service.ajustarStock(10L, -10);

        verify(kardexService).registrarMovimiento(eq(producto), eq(sucursal), eq("SALIDA"), eq(10), eq("AJUSTE_MANUAL"), isNull(), isNull());
    }

    @Test
    void ajustarStock_sinUsuarioNiSucursales_lanzaBusinessException() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserProvider.obtenerONull()).thenReturn(null);
        when(sucursalRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.ajustarStock(10L, 5))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void eliminar_existente_llamaDelete() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        service.eliminar(10L);

        verify(productoRepository).delete(producto);
    }
}
