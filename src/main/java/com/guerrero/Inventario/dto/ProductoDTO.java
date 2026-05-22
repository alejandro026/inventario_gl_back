package com.guerrero.Inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Datos de un producto del inventario")
public class ProductoDTO {

    @Schema(description = "Id (solo en respuestas)", example = "1")
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    @Schema(example = "Cuaderno profesional 100 hojas")
    private String nombre;

    @Size(max = 500)
    @Schema(example = "Cuaderno de pasta dura, raya")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Schema(example = "45.50")
    private Double precio;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    @Schema(example = "100")
    private Integer cantidad;

    @Min(value = 0, message = "El stock minimo no puede ser negativo")
    @Schema(example = "10")
    private Integer stockMinimo;

    @Schema(example = "true")
    private Boolean activo;

    @NotNull(message = "La categoria es obligatoria")
    @Schema(description = "Id de la categoria", example = "1")
    private Long categoriaId;

    @Schema(description = "Nombre de la categoria (solo en respuestas)", example = "PAPELERIA")
    private String categoriaNombre;
}
