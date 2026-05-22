package com.guerrero.Inventario.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credenciales de login")
public class LoginRequest {

    @NotBlank
    @Schema(example = "admin")
    private String username;

    @NotBlank
    @Schema(example = "admin123")
    private String password;
}
