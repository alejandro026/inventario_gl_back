package com.guerrero.Inventario.dto.auth;

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
@Schema(description = "Registro de un nuevo usuario")
public class RegistroRequest {

    @NotBlank
    @Size(max = 80)
    @Schema(example = "Alejandro Guerrero")
    private String nombre;

    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(example = "alejandro")
    private String username;

    @NotBlank
    @Email
    @Schema(example = "alejandro@papeleria.com")
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
    @Schema(example = "secreto123")
    private String password;

    @NotNull
    @Schema(example = "EMPLEADO")
    private Rol rol;
}
