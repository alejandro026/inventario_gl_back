package com.guerrero.Inventario.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de autenticacion JWT")
public class AuthResponse {

    @Schema(example = "Bearer")
    private String tokenType;

    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(example = "alejandro")
    private String username;

    @Schema(example = "ADMIN")
    private String rol;

    @Schema(description = "Tiempo de expiracion en milisegundos", example = "86400000")
    private Long expiresIn;
}
