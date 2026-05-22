package com.guerrero.Inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Categoria de producto (PAPELERIA, ELECTRONICA, RECARGAS, JUGUETES, ...)")
public class CategoriaDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60)
    @Schema(example = "PAPELERIA")
    private String nombre;

    @Size(max = 255)
    @Schema(example = "Articulos de oficina y escolares")
    private String descripcion;
}
