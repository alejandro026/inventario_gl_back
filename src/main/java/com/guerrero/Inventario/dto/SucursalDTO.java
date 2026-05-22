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
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Datos de una sucursal")
public class SucursalDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    @Schema(example = "Sucursal Centro")
    private String nombre;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 255)
    @Schema(example = "Av. Reforma 123, Mexico DF")
    private String direccion;

    @Size(max = 25)
    @Schema(example = "5512345678")
    private String telefono;
}
