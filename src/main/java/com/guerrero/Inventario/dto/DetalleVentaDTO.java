package com.guerrero.Inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Linea de detalle de una venta")
public class DetalleVentaDTO {

    private Long id;

    @NotNull(message = "El producto es obligatorio")
    @Schema(example = "1")
    private Long productoId;

    @Schema(description = "Nombre del producto (solo respuesta)", example = "Cuaderno profesional")
    private String nombreProd;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Schema(example = "2")
    private Integer cantProd;

    @Schema(description = "Precio unitario (calculado por el backend)", example = "45.50")
    private Double precio;

    @Schema(description = "Precio de compra unitario (calculado por el backend)", example = "25.00")
    private Double precioCompra;

    @Schema(description = "Subtotal (calculado por el backend)", example = "91.00")
    private Double subtotal;

    @Schema(description = "Nombre de la categoría del producto (solo respuesta)", example = "PAPELERIA")
    private String categoriaProd;
}
