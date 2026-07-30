package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.auth.AuthResponse;
import com.guerrero.Inventario.dto.auth.LoginRequest;
import com.guerrero.Inventario.dto.auth.RegistroRequest;
import com.guerrero.Inventario.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registro y login de usuarios")
@SecurityRequirements // endpoints publicos
public class AuthController {

    private final AuthService authService;
    private final boolean secureCookie;
    private final long refreshExpirationMs;

    public AuthController(AuthService authService,
                          @Value("${app.security.cookie.secure}") boolean secureCookie,
                          @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.authService = authService;
        this.secureCookie = secureCookie;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo usuario")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegistroRequest req, HttpServletResponse response) {
        AuthResponse authResponse = authService.registrar(req);
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Login y obtencion de token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(req);
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar el token de acceso")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = getRefreshTokenFromCookie(request);
        if (rawToken == null || rawToken.isEmpty()) {
            throw new org.springframework.security.authentication.BadCredentialsException("Refresh Token ausente o invalido");
        }
        AuthResponse authResponse = authService.refresh(rawToken);
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion e invalidar tokens")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = getRefreshTokenFromCookie(request);
        authService.logout(rawToken);
        addRefreshTokenCookie(response, null);
        return ResponseEntity.noContent().build();
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        long maxAge = (token == null) ? 0 : refreshExpirationMs / 1000;
        String tokenValue = (token == null) ? "" : token;

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenValue)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth") // solo enviado al path de refresco/logout
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
