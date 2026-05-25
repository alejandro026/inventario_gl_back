package com.guerrero.Inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO con métricas de rendimiento financiero y ganancias")
public class ReporteFinanceDTO {

    @Schema(description = "Ingresos brutos totales por ventas", example = "15450.50")
    private Double totalVentas;

    @Schema(description = "Costo de inversión total en mercancía vendida", example = "9200.00")
    private Double totalCosto;

    @Schema(description = "Ganancia neta total (ingresos - costo)", example = "6250.50")
    private Double gananciaNeta;

    @Schema(description = "Margen de ganancia promedio del negocio", example = "40.45")
    private Double margenUtilidad;

    @Schema(description = "Cantidad total de transacciones completadas", example = "120")
    private Long cantidadVentas;

    @Schema(description = "Ranking de productos con mejor rendimiento")
    private List<ProductoRanking> rankingProductos;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductoRanking {
        private Long productoId;
        private String nombre;
        private Integer cantidadVendida;
        private Double totalIngresos;
        private Double totalCosto;
        private Double totalGanancia;
        private Double margenUtilidad;
    }
}
