package com.guerrero.Inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para los renglones/detalles de una cotización")
public class DetalleCotizacionDTO {

    @Schema(description = "ID del renglón de detalle", example = "10")
    private Long id;

    @NotNull(message = "El producto es obligatorio")
    @Schema(description = "ID del producto", example = "20")
    private Long productoId;

    @Schema(description = "Nombre del producto", example = "Libreta Cuadro Swinig")
    private String nombreProd;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima debe ser 1")
    @Schema(description = "Cantidad de unidades del producto", example = "3")
    private Integer cantProd;

    @Schema(description = "Precio unitario del producto cotizado", example = "30.00")
    private BigDecimal precio;

    @Schema(description = "Subtotal de este renglón (cantidad * precio)", example = "90.00")
    private BigDecimal subtotal;

    @Schema(description = "ID de la categoría del producto", example = "5")
    private Long categoriaId;

    @Schema(description = "Nombre de la categoría del producto", example = "Papelería")
    private String categoriaNombre;
}
