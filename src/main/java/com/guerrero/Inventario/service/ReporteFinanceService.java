package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.ReporteFinanceDTO;
import com.guerrero.Inventario.model.DetalleVenta;
import com.guerrero.Inventario.model.Venta;
import com.guerrero.Inventario.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        double totalVentas = 0.0;
        double totalCosto = 0.0;
        long cantidadVentasConFiltro = 0;

        // Mapa para agrupar ventas por producto (con filtro de categoría si está especificado)
        Map<Long, ReporteFinanceDTO.ProductoRanking> rankingMap = new HashMap<>();
        
        // Mapa para agrupar rendimiento general por todas las categorías
        Map<Long, ReporteFinanceDTO.CategoriaGanancia> categoriasMap = new HashMap<>();

        for (Venta v : ventas) {
            boolean ventaTieneProductoDeCategoria = false;
            double subtotalVentaFiltro = 0.0;
            double costoVentaFiltro = 0.0;

            for (DetalleVenta d : v.getDetalle()) {
                double precioVenta = d.getPrecio() != null ? d.getPrecio() : 0.0;
                double precioCompra = d.getPrecioCompra() != null ? d.getPrecioCompra() : 
                                     (d.getProducto() != null && d.getProducto().getPrecioCompra() != null ? d.getProducto().getPrecioCompra() : 0.0);
                
                int cantidad = d.getCantProd() != null ? d.getCantProd() : 0;

                double costoFila = precioCompra * cantidad;
                double subtotalFila = d.getSubtotal() != null ? d.getSubtotal() : (precioVenta * cantidad);

                // 1. Agrupar en el rendimiento por categoría general (independiente del filtro seleccionado)
                if (d.getProducto() != null && d.getProducto().getCategoria() != null) {
                    Long catId = d.getProducto().getCategoria().getId();
                    String catNombre = d.getProducto().getCategoria().getNombre();

                    ReporteFinanceDTO.CategoriaGanancia cg = categoriasMap.computeIfAbsent(catId, id -> {
                        ReporteFinanceDTO.CategoriaGanancia c = new ReporteFinanceDTO.CategoriaGanancia();
                        c.setCategoriaId(id);
                        c.setNombre(catNombre);
                        c.setTotalIngresos(0.0);
                        c.setTotalCosto(0.0);
                        c.setTotalGanancia(0.0);
                        c.setMargenUtilidad(0.0);
                        return c;
                    });

                    cg.setTotalIngresos(cg.getTotalIngresos() + subtotalFila);
                    cg.setTotalCosto(cg.getTotalCosto() + costoFila);
                    cg.setTotalGanancia(cg.getTotalIngresos() - cg.getTotalCosto());
                    if (cg.getTotalIngresos() > 0) {
                        cg.setMargenUtilidad((cg.getTotalGanancia() / cg.getTotalIngresos()) * 100);
                    }
                }

                // 2. Aplicar filtro de categoría a las métricas del dashboard si se especifica
                boolean cumpleFiltro = (categoriaId == null) || 
                    (d.getProducto() != null && d.getProducto().getCategoria() != null && d.getProducto().getCategoria().getId().equals(categoriaId));

                if (cumpleFiltro) {
                    subtotalVentaFiltro += subtotalFila;
                    costoVentaFiltro += costoFila;
                    ventaTieneProductoDeCategoria = true;

                    if (d.getProducto() != null) {
                        Long prodId = d.getProducto().getId();
                        String nombre = d.getProducto().getNombre();

                        ReporteFinanceDTO.ProductoRanking r = rankingMap.computeIfAbsent(prodId, id -> {
                            ReporteFinanceDTO.ProductoRanking pr = new ReporteFinanceDTO.ProductoRanking();
                            pr.setProductoId(id);
                            pr.setNombre(nombre);
                            pr.setCantidadVendida(0);
                            pr.setTotalIngresos(0.0);
                            pr.setTotalCosto(0.0);
                            pr.setTotalGanancia(0.0);
                            pr.setMargenUtilidad(0.0);
                            return pr;
                        });

                        r.setCantidadVendida(r.getCantidadVendida() + cantidad);
                        r.setTotalIngresos(r.getTotalIngresos() + subtotalFila);
                        r.setTotalCosto(r.getTotalCosto() + costoFila);
                        r.setTotalGanancia(r.getTotalIngresos() - r.getTotalCosto());
                        
                        if (r.getTotalIngresos() > 0) {
                            r.setMargenUtilidad((r.getTotalGanancia() / r.getTotalIngresos()) * 100);
                        }
                    }
                }
            }

            if (ventaTieneProductoDeCategoria) {
                cantidadVentasConFiltro++;
                if (categoriaId != null) {
                    totalVentas += subtotalVentaFiltro;
                    totalCosto += costoVentaFiltro;
                } else {
                    totalVentas += v.getTotal() != null ? v.getTotal() : 0.0;
                    totalCosto += costoVentaFiltro;
                }
            }
        }

        double gananciaNeta = totalVentas - totalCosto;
        double margenUtilidad = totalVentas > 0 ? (gananciaNeta / totalVentas) * 100 : 0.0;

        // Ordenar productos del ranking de mayor a menor ganancia
        List<ReporteFinanceDTO.ProductoRanking> rankingOrdenado = rankingMap.values().stream()
                .sorted(Comparator.comparingDouble(ReporteFinanceDTO.ProductoRanking::getTotalGanancia).reversed())
                .limit(10) // Top 10 productos más rentables
                .collect(Collectors.toList());

        // Ordenar categorías por mayor ganancia
        List<ReporteFinanceDTO.CategoriaGanancia> rendimientoCategorias = categoriasMap.values().stream()
                .sorted(Comparator.comparingDouble(ReporteFinanceDTO.CategoriaGanancia::getTotalGanancia).reversed())
                .collect(Collectors.toList());

        ReporteFinanceDTO dto = new ReporteFinanceDTO();
        dto.setTotalVentas(totalVentas);
        dto.setTotalCosto(totalCosto);
        dto.setGananciaNeta(gananciaNeta);
        dto.setMargenUtilidad(margenUtilidad);
        dto.setCantidadVentas(categoriaId != null ? cantidadVentasConFiltro : (long) ventas.size());
        dto.setRankingProductos(rankingOrdenado);
        dto.setRendimientoCategorias(rendimientoCategorias);

        return dto;
    }
}
