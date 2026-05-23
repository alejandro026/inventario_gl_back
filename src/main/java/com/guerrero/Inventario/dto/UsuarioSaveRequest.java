package com.guerrero.Inventario.dto;

import com.guerrero.Inventario.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para crear o actualizar un usuario")
public class UsuarioSaveRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    @Schema(example = "Juan Pérez")
    private String nombre;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    @Schema(example = "juan.perez")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser una dirección válida")
    @Size(max = 120, message = "El email no puede superar los 120 caracteres")
    @Schema(example = "juan.perez@example.com")
    private String email;

    @Schema(description = "Contraseña. Obligatoria para crear, opcional para actualizar (si se deja vacía, no se cambia)", example = "clave123")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    @Schema(example = "EMPLEADO")
    private Rol rol;

    @NotNull(message = "El estado de activo es obligatorio")
    @Schema(example = "true")
    private Boolean activo;
}
