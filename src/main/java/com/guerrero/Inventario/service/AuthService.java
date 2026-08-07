package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.auth.AuthResponse;
import com.guerrero.Inventario.dto.auth.LoginRequest;
import com.guerrero.Inventario.dto.auth.RegistroRequest;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.model.RefreshToken;
import com.guerrero.Inventario.repository.UsuarioRepository;
import com.guerrero.Inventario.repository.RefreshTokenRepository;
import com.guerrero.Inventario.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository,
                       @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    private RefreshToken generarYGuardarRefreshToken(Usuario usuario, String familyId) {
        String tokenStr = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .token(tokenStr)
                .usuario(usuario)
                .fechaExpiracion(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revocado(false)
                .used(false)
                .tokenFamilyId(familyId != null ? familyId : UUID.randomUUID().toString())
                .build();
        return refreshTokenRepository.save(rt);
    }

    public AuthResponse registrar(RegistroRequest req) {
        log.info("Registrando nuevo usuario. Username: {}, Rol: {}", req.getUsername(), req.getRol());
        if (usuarioRepository.existsByUsername(req.getUsername())) {
            log.warn("Registro fallido: El username '{}' ya existe.", req.getUsername());
            throw new DuplicateResourceException("El username ya esta registrado");
        }
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            log.warn("Registro fallido: El email '{}' ya existe.", req.getEmail());
            throw new DuplicateResourceException("El email ya esta registrado");
        }
        Usuario u = new Usuario();
        u.setNombre(req.getNombre());
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRol(req.getRol());
        u.setActivo(Boolean.TRUE);
        Usuario guardado = usuarioRepository.save(u);

        String token = jwtService.generateToken(guardado);
        RefreshToken rt = generarYGuardarRefreshToken(guardado, null);

        log.info("Usuario '{}' registrado exitosamente. ID: {}", guardado.getUsername(), guardado.getId());
        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(token)
                .refreshToken(rt.getToken())
                .username(guardado.getUsername())
                .rol(guardado.getRol().name())
                .expiresIn(jwtService.getExpirationMs())
                .sucursalId(guardado.getSucursal() != null ? guardado.getSucursal().getId() : null)
                .sucursalNombre(guardado.getSucursal() != null ? guardado.getSucursal().getNombre() : null)
                .usuarioId(guardado.getId())
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        log.info("Intento de login para usuario: {}", req.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );
        } catch (Exception e) {
            log.warn("Fallo de autenticación para usuario: {}. Causa: {}", req.getUsername(), e.getMessage());
            throw e;
        }

        Usuario u = usuarioRepository.findByUsername(req.getUsername()).orElseThrow();
        String token = jwtService.generateToken(u);
        RefreshToken rt = generarYGuardarRefreshToken(u, null);

        log.info("Login exitoso para usuario: {}. ID Usuario: {}, Rol: {}, Sucursal: {}", 
                u.getUsername(), u.getId(), u.getRol().name(), u.getSucursal() != null ? u.getSucursal().getId() : "Ninguna");

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(token)
                .refreshToken(rt.getToken())
                .username(u.getUsername())
                .rol(u.getRol().name())
                .expiresIn(jwtService.getExpirationMs())
                .sucursalId(u.getSucursal() != null ? u.getSucursal().getId() : null)
                .sucursalNombre(u.getSucursal() != null ? u.getSucursal().getNombre() : null)
                .usuarioId(u.getId())
                .build();
    }

    public AuthResponse refresh(String rawToken) {
        log.info("Procesando solicitud de refresco de token.");
        RefreshToken rt = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> {
                    log.warn("Fallo de refresco: token ausente o inválido en base de datos.");
                    return new org.springframework.security.authentication.BadCredentialsException("Refresh Token invalido");
                });

        // Detectar reuso / robo de token
        if (rt.isUsed() || rt.isRevocado()) {
            log.warn("¡ALERTA DE SEGURIDAD! Se detectó un intento de reúso/robo del Refresh Token. Se invalida la familia completa. FamilyId: {}", rt.getTokenFamilyId());
            refreshTokenRepository.revokeFamily(rt.getTokenFamilyId());
            throw new org.springframework.security.authentication.BadCredentialsException("Refresh Token ya utilizado o revocado. Alerta de seguridad.");
        }

        // Detectar expiracion
        if (rt.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            log.warn("Fallo de refresco: Token expirado para usuario: {}.", rt.getUsuario().getUsername());
            throw new org.springframework.security.authentication.BadCredentialsException("Refresh Token expirado");
        }

        // Marcar token anterior como utilizado
        rt.setUsed(true);
        refreshTokenRepository.save(rt);

        // Generar nuevo par de tokens (RTR)
        Usuario usuario = rt.getUsuario();
        String newAccessToken = jwtService.generateToken(usuario);
        RefreshToken newRefreshToken = generarYGuardarRefreshToken(usuario, rt.getTokenFamilyId());

        log.info("Token rotado exitosamente (RTR) para usuario: {}. FamilyId: {}", usuario.getUsername(), rt.getTokenFamilyId());

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .username(usuario.getUsername())
                .rol(usuario.getRol().name())
                .expiresIn(jwtService.getExpirationMs())
                .sucursalId(usuario.getSucursal() != null ? usuario.getSucursal().getId() : null)
                .sucursalNombre(usuario.getSucursal() != null ? usuario.getSucursal().getNombre() : null)
                .usuarioId(usuario.getId())
                .build();
    }

    public void logout(String rawToken) {
        if (rawToken != null) {
            log.info("Procesando logout. Invalidando token.");
            refreshTokenRepository.findByToken(rawToken).ifPresent(rt -> {
                rt.setRevocado(true);
                refreshTokenRepository.save(rt);
                log.info("Token de refresco revocado para el usuario: {}", rt.getUsuario().getUsername());
            });
        } else {
            log.info("Petición de logout recibida sin token de refresco.");
        }
    }
}
