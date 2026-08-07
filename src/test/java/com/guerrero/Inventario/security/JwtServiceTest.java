package com.guerrero.Inventario.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes());

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 600_000L, "test-issuer");
    }

    private UserDetails userDetails(String username) {
        return User.withUsername(username).password("x").authorities(List.of()).build();
    }

    @Test
    void generateTokenYExtractUsername_devuelveElMismoUsername() {
        UserDetails user = userDetails("alejandro");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alejandro");
    }

    @Test
    void isTokenValid_mismoUsuarioYNoExpirado_true() {
        UserDetails user = userDetails("alejandro");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_usuarioDistinto_false() {
        UserDetails user = userDetails("alejandro");
        UserDetails otro = userDetails("otro-usuario");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, otro)).isFalse();
    }

    @Test
    void isTokenValid_tokenExpirado_false() {
        JwtService servicioExpirado = new JwtService(SECRET, -1000L, "test-issuer");
        UserDetails user = userDetails("alejandro");
        String token = servicioExpirado.generateToken(user);

        assertThat(servicioExpirado.isTokenValid(token, user)).isFalse();
    }

    @Test
    void isTokenValid_tokenMalformado_false() {
        UserDetails user = userDetails("alejandro");

        assertThat(jwtService.isTokenValid("esto-no-es-un-jwt", user)).isFalse();
    }

    @Test
    void isTokenValid_firmadoConOtraClave_false() {
        JwtService otroServicio = new JwtService(
                Base64.getEncoder().encodeToString("otra-clave-de-32-bytes-diferente".getBytes()),
                600_000L, "test-issuer");
        UserDetails user = userDetails("alejandro");
        String token = otroServicio.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    void getExpirationMs_devuelveElValorConfigurado() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(600_000L);
    }
}
