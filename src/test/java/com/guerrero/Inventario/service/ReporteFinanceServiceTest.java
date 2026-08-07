package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.ReporteFinanceDTO;
import com.guerrero.Inventario.model.*;
import com.guerrero.Inventario.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteFinanceServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private ReporteFinanceService service;

    private Categoria categoria(long id, String nombre) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        return c;
    }

    private Producto producto(long id, String nombre, Categoria categoria) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setCategoria(categoria);
        return p;
    }

    private DetalleVenta detalle(Producto producto, int cantidad, BigDecimal precio, BigDecimal precioCompra) {
        DetalleVenta d = new DetalleVenta();
        d.setProducto(producto);
        d.setCantProd(cantidad);
        d.setPrecio(precio);
        d.setPrecioCompra(precioCompra);
        d.setSubtotal(precio.multiply(BigDecimal.valueOf(cantidad)));
        return d;
    }

    private Venta venta(BigDecimal total, DetalleVenta... detalles) {
        Venta v = new Venta();
        v.setId((long) (Math.random() * 100000));
        v.setEstado(Venta.EstadoVenta.COMPLETADA);
        v.setTotal(total);
        v.setDetalle(new ArrayList<>(List.of(detalles)));
        return v;
    }

    @Test
    void obtenerMetricasFinancieras_sinVentas_devuelveCeros() {
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fin = LocalDateTime.now();
        when(ventaRepository.findCompletedSalesBetween(inicio, fin)).thenReturn(List.of());

        ReporteFinanceDTO dto = service.obtenerMétricasFinancieras(inicio, fin, null);

        assertThat(dto.getTotalVentas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getGananciaNeta()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getMargenUtilidad()).isEqualTo(0.0);
        assertThat(dto.getCantidadVentas()).isEqualTo(0L);
        assertThat(dto.getRankingProductos()).isEmpty();
    }

    @Test
    void obtenerMetricasFinancieras_sinFiltro_calculaTotalesYGanancia() {
        Categoria cat = categoria(1L, "PAPELERIA");
        Producto prod = producto(10L, "Cuaderno", cat);
        DetalleVenta d = detalle(prod, 2, new BigDecimal("50.00"), new BigDecimal("30.00"));
        Venta v = venta(new BigDecimal("100.00"), d);

        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fin = LocalDateTime.now();
        when(ventaRepository.findCompletedSalesBetween(inicio, fin)).thenReturn(List.of(v));

        ReporteFinanceDTO dto = service.obtenerMétricasFinancieras(inicio, fin, null);

        assertThat(dto.getTotalVentas()).isEqualByComparingTo("100.00");
        assertThat(dto.getTotalCosto()).isEqualByComparingTo("60.00");
        assertThat(dto.getGananciaNeta()).isEqualByComparingTo("40.00");
        assertThat(dto.getMargenUtilidad()).isEqualTo(40.0);
        assertThat(dto.getCantidadVentas()).isEqualTo(1L);
        assertThat(dto.getRankingProductos()).hasSize(1);
        assertThat(dto.getRankingProductos().get(0).getTotalGanancia()).isEqualByComparingTo("40.00");
        assertThat(dto.getRendimientoCategorias()).hasSize(1);
    }

    @Test
    void obtenerMetricasFinancieras_conFiltroCategoria_excluyeOtrasCategorias() {
        Categoria papeleria = categoria(1L, "PAPELERIA");
        Categoria electronica = categoria(2L, "ELECTRONICA");
        Producto cuaderno = producto(10L, "Cuaderno", papeleria);
        Producto cargador = producto(11L, "Cargador", electronica);

        DetalleVenta d1 = detalle(cuaderno, 1, new BigDecimal("50.00"), new BigDecimal("30.00"));
        DetalleVenta d2 = detalle(cargador, 1, new BigDecimal("200.00"), new BigDecimal("120.00"));
        Venta v = venta(new BigDecimal("250.00"), d1, d2);

        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fin = LocalDateTime.now();
        when(ventaRepository.findCompletedSalesBetween(inicio, fin)).thenReturn(List.of(v));

        ReporteFinanceDTO dto = service.obtenerMétricasFinancieras(inicio, fin, 1L);

        // Solo se contabiliza la linea de PAPELERIA
        assertThat(dto.getTotalVentas()).isEqualByComparingTo("50.00");
        assertThat(dto.getTotalCosto()).isEqualByComparingTo("30.00");
        assertThat(dto.getRankingProductos()).hasSize(1);
        assertThat(dto.getRankingProductos().get(0).getNombre()).isEqualTo("Cuaderno");
        // El desglose por categoria sigue reportando ambas, independiente del filtro
        assertThat(dto.getRendimientoCategorias()).hasSize(2);
    }

    @Test
    void obtenerMetricasFinancieras_rankingLimitadoYOrdenadoPorGanancia() {
        Categoria cat = categoria(1L, "PAPELERIA");
        List<Venta> ventas = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Producto p = producto(i, "Producto" + i, cat);
            DetalleVenta d = detalle(p, 1, BigDecimal.valueOf(i * 10), BigDecimal.valueOf(i));
            ventas.add(venta(BigDecimal.valueOf(i * 10), d));
        }

        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fin = LocalDateTime.now();
        when(ventaRepository.findCompletedSalesBetween(inicio, fin)).thenReturn(ventas);

        ReporteFinanceDTO dto = service.obtenerMétricasFinancieras(inicio, fin, null);

        assertThat(dto.getRankingProductos()).hasSize(10);
        // El producto 12 (mayor ganancia) debe ir primero
        assertThat(dto.getRankingProductos().get(0).getNombre()).isEqualTo("Producto12");
    }
}
