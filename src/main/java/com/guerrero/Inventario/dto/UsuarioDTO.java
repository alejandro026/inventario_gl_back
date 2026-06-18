package com.guerrero.Inventario.dto;

import com.guerrero.Inventario.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Información del usuario expuesta para administración")
public class UsuarioDTO {

    private Long id;

    @Schema(example = "Juan Pérez")
    private String nombre;

    @Schema(example = "juan.perez")
    private String username;

    @Schema(example = "juan.perez@example.com")
    private String email;

    @Schema(example = "EMPLEADO")
    private Rol rol;

    @Schema(example = "true")
    private Boolean activo;

    @Schema(example = "1")
    private Long sucursalId;

    @Schema(example = "Sucursal Central")
    private String sucursalNombre;
}
