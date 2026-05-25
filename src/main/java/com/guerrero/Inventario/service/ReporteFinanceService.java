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

    public ReporteFinanceDTO obtenerMétricasFinancieras(LocalDateTime inicio, LocalDateTime fin) {
        List<Venta> ventas = ventaRepository.findCompletedSalesBetween(inicio, fin);

        double totalVentas = 0.0;
        double totalCosto = 0.0;
        long cantidadVentas = ventas.size();

        // Mapa para agrupar ventas por producto
        Map<Long, ReporteFinanceDTO.ProductoRanking> rankingMap = new HashMap<>();

        for (Venta v : ventas) {
            totalVentas += v.getTotal() != null ? v.getTotal() : 0.0;

            for (DetalleVenta d : v.getDetalle()) {
                double precioVenta = d.getPrecio() != null ? d.getPrecio() : 0.0;
                double precioCompra = d.getPrecioCompra() != null ? d.getPrecioCompra() : 
                                     (d.getProducto() != null && d.getProducto().getPrecioCompra() != null ? d.getProducto().getPrecioCompra() : 0.0);
                
                int cantidad = d.getCantProd() != null ? d.getCantProd() : 0;

                double costoFila = precioCompra * cantidad;
                double subtotalFila = d.getSubtotal() != null ? d.getSubtotal() : (precioVenta * cantidad);

                totalCosto += costoFila;

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

        double gananciaNeta = totalVentas - totalCosto;
        double margenUtilidad = totalVentas > 0 ? (gananciaNeta / totalVentas) * 100 : 0.0;

        // Ordenar productos del ranking de mayor a menor ganancia
        List<ReporteFinanceDTO.ProductoRanking> rankingOrdenado = rankingMap.values().stream()
                .sorted(Comparator.comparingDouble(ReporteFinanceDTO.ProductoRanking::getTotalGanancia).reversed())
                .limit(10) // Top 10 productos más rentables
                .collect(Collectors.toList());

        ReporteFinanceDTO dto = new ReporteFinanceDTO();
        dto.setTotalVentas(totalVentas);
        dto.setTotalCosto(totalCosto);
        dto.setGananciaNeta(gananciaNeta);
        dto.setMargenUtilidad(margenUtilidad);
        dto.setCantidadVentas(cantidadVentas);
        dto.setRankingProductos(rankingOrdenado);

        return dto;
    }
}
