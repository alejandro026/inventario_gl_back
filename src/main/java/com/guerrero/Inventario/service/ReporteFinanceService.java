package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.ReporteFinanceDTO;
import com.guerrero.Inventario.model.DetalleVenta;
import com.guerrero.Inventario.model.Venta;
import com.guerrero.Inventario.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReporteFinanceService {

    private final VentaRepository ventaRepository;

    public ReporteFinanceService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    public ReporteFinanceDTO obtenerMétricasFinancieras(LocalDateTime inicio, LocalDateTime fin, Long categoriaId) {
        List<Venta> ventas = ventaRepository.findCompletedSalesBetween(inicio, fin);

        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal totalCosto = BigDecimal.ZERO;
        BigDecimal totalDescuentos = BigDecimal.ZERO;
        long cantidadVentasConFiltro = 0;

        // Mapa para agrupar ventas por producto (con filtro de categoría si está especificado)
        Map<Long, ReporteFinanceDTO.ProductoRanking> rankingMap = new HashMap<>();

        // Mapa para agrupar rendimiento general por todas las categorías
        Map<Long, ReporteFinanceDTO.CategoriaGanancia> categoriasMap = new HashMap<>();

        for (Venta v : ventas) {
            boolean ventaTieneProductoDeCategoria = false;
            BigDecimal subtotalVentaFiltro = BigDecimal.ZERO;
            BigDecimal costoVentaFiltro = BigDecimal.ZERO;

            for (DetalleVenta d : v.getDetalle()) {
                BigDecimal precioVenta = d.getPrecio() != null ? d.getPrecio() : BigDecimal.ZERO;
                BigDecimal precioCompra = d.getPrecioCompra() != null ? d.getPrecioCompra() :
                                     (d.getProducto() != null && d.getProducto().getPrecioCompra() != null ? d.getProducto().getPrecioCompra() : BigDecimal.ZERO);

                int cantidad = d.getCantProd() != null ? d.getCantProd() : 0;
                BigDecimal cantidadBd = BigDecimal.valueOf(cantidad);

                BigDecimal costoFila = precioCompra.multiply(cantidadBd);
                BigDecimal subtotalFila = d.getSubtotal() != null ? d.getSubtotal() : precioVenta.multiply(cantidadBd);

                // 1. Agrupar en el rendimiento por categoría general (independiente del filtro seleccionado)
                if (d.getProducto() != null && d.getProducto().getCategoria() != null) {
                    Long catId = d.getProducto().getCategoria().getId();
                    String catNombre = d.getProducto().getCategoria().getNombre();

                    ReporteFinanceDTO.CategoriaGanancia cg = categoriasMap.computeIfAbsent(catId, id -> {
                        ReporteFinanceDTO.CategoriaGanancia c = new ReporteFinanceDTO.CategoriaGanancia();
                        c.setCategoriaId(id);
                        c.setNombre(catNombre);
                        c.setTotalIngresos(BigDecimal.ZERO);
                        c.setTotalCosto(BigDecimal.ZERO);
                        c.setTotalGanancia(BigDecimal.ZERO);
                        c.setMargenUtilidad(0.0);
                        return c;
                    });

                    cg.setTotalIngresos(cg.getTotalIngresos().add(subtotalFila));
                    cg.setTotalCosto(cg.getTotalCosto().add(costoFila));
                    cg.setTotalGanancia(cg.getTotalIngresos().subtract(cg.getTotalCosto()));
                    if (cg.getTotalIngresos().compareTo(BigDecimal.ZERO) > 0) {
                        cg.setMargenUtilidad(cg.getTotalGanancia().doubleValue() / cg.getTotalIngresos().doubleValue() * 100);
                    }
                }

                // 2. Aplicar filtro de categoría a las métricas del dashboard si se especifica
                boolean cumpleFiltro = (categoriaId == null) ||
                    (d.getProducto() != null && d.getProducto().getCategoria() != null && d.getProducto().getCategoria().getId().equals(categoriaId));

                if (cumpleFiltro) {
                    subtotalVentaFiltro = subtotalVentaFiltro.add(subtotalFila);
                    costoVentaFiltro = costoVentaFiltro.add(costoFila);
                    ventaTieneProductoDeCategoria = true;

                    if (d.getProducto() != null) {
                        Long prodId = d.getProducto().getId();
                        String nombre = d.getProducto().getNombre();

                        ReporteFinanceDTO.ProductoRanking r = rankingMap.computeIfAbsent(prodId, id -> {
                            ReporteFinanceDTO.ProductoRanking pr = new ReporteFinanceDTO.ProductoRanking();
                            pr.setProductoId(id);
                            pr.setNombre(nombre);
                            pr.setCantidadVendida(0);
                            pr.setTotalIngresos(BigDecimal.ZERO);
                            pr.setTotalCosto(BigDecimal.ZERO);
                            pr.setTotalGanancia(BigDecimal.ZERO);
                            pr.setMargenUtilidad(0.0);
                            return pr;
                        });

                        r.setCantidadVendida(r.getCantidadVendida() + cantidad);
                        r.setTotalIngresos(r.getTotalIngresos().add(subtotalFila));
                        r.setTotalCosto(r.getTotalCosto().add(costoFila));
                        r.setTotalGanancia(r.getTotalIngresos().subtract(r.getTotalCosto()));

                        if (r.getTotalIngresos().compareTo(BigDecimal.ZERO) > 0) {
                            r.setMargenUtilidad(r.getTotalGanancia().doubleValue() / r.getTotalIngresos().doubleValue() * 100);
                        }
                    }
                }
            }

            if (ventaTieneProductoDeCategoria) {
                cantidadVentasConFiltro++;
                if (v.getDescuento() != null) {
                    totalDescuentos = totalDescuentos.add(v.getDescuento());
                }
                if (categoriaId != null) {
                    totalVentas = totalVentas.add(subtotalVentaFiltro);
                    totalCosto = totalCosto.add(costoVentaFiltro);
                } else {
                    totalVentas = totalVentas.add(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO);
                    totalCosto = totalCosto.add(costoVentaFiltro);
                }
            }
        }

        BigDecimal gananciaNeta = totalVentas.subtract(totalCosto);
        double margenUtilidad = totalVentas.compareTo(BigDecimal.ZERO) > 0
                ? gananciaNeta.doubleValue() / totalVentas.doubleValue() * 100
                : 0.0;

        // Ordenar productos del ranking de mayor a menor ganancia
        List<ReporteFinanceDTO.ProductoRanking> rankingOrdenado = rankingMap.values().stream()
                .sorted(Comparator.comparing(ReporteFinanceDTO.ProductoRanking::getTotalGanancia).reversed())
                .limit(10) // Top 10 productos más rentables
                .collect(Collectors.toList());

        // Ordenar categorías por mayor ganancia
        List<ReporteFinanceDTO.CategoriaGanancia> rendimientoCategorias = categoriasMap.values().stream()
                .sorted(Comparator.comparing(ReporteFinanceDTO.CategoriaGanancia::getTotalGanancia).reversed())
                .collect(Collectors.toList());

        ReporteFinanceDTO dto = new ReporteFinanceDTO();
        dto.setTotalVentas(totalVentas);
        dto.setTotalCosto(totalCosto);
        dto.setGananciaNeta(gananciaNeta);
        dto.setMargenUtilidad(margenUtilidad);
        dto.setTotalDescuentos(totalDescuentos);
        dto.setCantidadVentas(categoriaId != null ? cantidadVentasConFiltro : (long) ventas.size());
        dto.setRankingProductos(rankingOrdenado);
        dto.setRendimientoCategorias(rendimientoCategorias);

        return dto;
    }
}
