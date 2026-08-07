package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.auth.AuthResponse;
import com.guerrero.Inventario.dto.auth.LoginRequest;
import com.guerrero.Inventario.dto.auth.RegistroRequest;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.model.RefreshToken;
import com.guerrero.Inventario.model.Rol;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.RefreshTokenRepository;
import com.guerrero.Inventario.repository.UsuarioRepository;
import com.guerrero.Inventario.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService service;

    private static final long REFRESH_EXP_MS = 604_800_000L;

    @BeforeEach
    void setUp() {
        service = new AuthService(usuarioRepository, passwordEncoder, authenticationManager,
                jwtService, refreshTokenRepository, REFRESH_EXP_MS);
    }

    private Usuario usuarioGuardado() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Alejandro");
        u.setUsername("alejandro");
        u.setEmail("alejandro@test.com");
        u.setRol(Rol.EMPLEADO);
        u.setActivo(true);
        return u;
    }

    @Test
    void registrar_usernameExistente_lanzaDuplicateResourceException() {
        RegistroRequest req = new RegistroRequest("Alejandro", "alejandro", "a@test.com", "secreto123", Rol.EMPLEADO);
        when(usuarioRepository.existsByUsername("alejandro")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(req)).isInstanceOf(DuplicateResourceException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrar_emailExistente_lanzaDuplicateResourceException() {
        RegistroRequest req = new RegistroRequest("Alejandro", "alejandro", "a@test.com", "secreto123", Rol.EMPLEADO);
        when(usuarioRepository.existsByUsername("alejandro")).thenReturn(false);
        when(usuarioRepository.existsByEmail("a@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void registrar_exitoso_devuelveAuthResponseConTokens() {
        RegistroRequest req = new RegistroRequest("Alejandro", "alejandro", "a@test.com", "secreto123", Rol.EMPLEADO);
        Usuario guardado = usuarioGuardado();

        when(usuarioRepository.existsByUsername("alejandro")).thenReturn(false);
        when(usuarioRepository.existsByEmail("a@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secreto123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);
        when(jwtService.generateToken(guardado)).thenReturn("access-token");
        when(jwtService.getExpirationMs()).thenReturn(600000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.registrar(req);

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
        assertThat(resp.getUsername()).isEqualTo("alejandro");
        assertThat(resp.getRefreshToken()).isNotBlank();
    }

    @Test
    void login_credencialesValidas_devuelveTokens() {
        LoginRequest req = new LoginRequest("alejandro", "secreto123");
        Usuario u = usuarioGuardado();

        when(usuarioRepository.findByUsername("alejandro")).thenReturn(Optional.of(u));
        when(jwtService.generateToken(u)).thenReturn("access-token");
        when(jwtService.getExpirationMs()).thenReturn(600000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.login(req);

        assertThat(resp.getAccessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_credencialesInvalidas_propagaExcepcion() {
        LoginRequest req = new LoginRequest("alejandro", "malapass");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Credenciales invalidas"));

        assertThatThrownBy(() -> service.login(req)).isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    void refresh_tokenInexistente_lanzaBadCredentials() {
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("abc")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_tokenYaUsado_revocaFamiliaYLanzaExcepcion() {
        RefreshToken rt = RefreshToken.builder()
                .token("abc")
                .used(true)
                .revocado(false)
                .tokenFamilyId("fam-1")
                .fechaExpiracion(LocalDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> service.refresh("abc")).isInstanceOf(BadCredentialsException.class);
        verify(refreshTokenRepository).revokeFamily("fam-1");
    }

    @Test
    void refresh_tokenExpirado_lanzaExcepcion() {
        Usuario u = usuarioGuardado();
        RefreshToken rt = RefreshToken.builder()
                .token("abc")
                .used(false)
                .revocado(false)
                .tokenFamilyId("fam-1")
                .usuario(u)
                .fechaExpiracion(LocalDateTime.now().minusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> service.refresh("abc")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_exitoso_rotaTokenYMarcaAnteriorComoUsado() {
        Usuario u = usuarioGuardado();
        RefreshToken rt = RefreshToken.builder()
                .token("abc")
                .used(false)
                .revocado(false)
                .tokenFamilyId("fam-1")
                .usuario(u)
                .fechaExpiracion(LocalDateTime.now().plusDays(1))
                .build();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(rt));
        when(jwtService.generateToken(u)).thenReturn("new-access-token");
        when(jwtService.getExpirationMs()).thenReturn(600000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.refresh("abc");

        assertThat(resp.getAccessToken()).isEqualTo("new-access-token");
        assertThat(rt.isUsed()).isTrue();
    }

    @Test
    void logout_conToken_loRevoca() {
        RefreshToken rt = RefreshToken.builder().token("abc").revocado(false).usuario(usuarioGuardado()).build();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(rt));

        service.logout("abc");

        assertThat(rt.isRevocado()).isTrue();
        verify(refreshTokenRepository).save(rt);
    }

    @Test
    void logout_sinToken_noInteractuaConRepositorio() {
        service.logout(null);

        verifyNoInteractions(refreshTokenRepository);
    }
}
