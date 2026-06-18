package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.auth.AuthResponse;
import com.guerrero.Inventario.dto.auth.LoginRequest;
import com.guerrero.Inventario.dto.auth.RegistroRequest;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.UsuarioRepository;
import com.guerrero.Inventario.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse registrar(RegistroRequest req) {
        if (usuarioRepository.existsByUsername(req.getUsername())) {
            throw new DuplicateResourceException("El username ya esta registrado");
        }
        if (usuarioRepository.existsByEmail(req.getEmail())) {
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
        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(token)
                .username(guardado.getUsername())
                .rol(guardado.getRol().name())
                .expiresIn(jwtService.getExpirationMs())
                .sucursalId(guardado.getSucursal() != null ? guardado.getSucursal().getId() : null)
                .sucursalNombre(guardado.getSucursal() != null ? guardado.getSucursal().getNombre() : null)
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        Usuario u = usuarioRepository.findByUsername(req.getUsername()).orElseThrow();
        String token = jwtService.generateToken(u);
        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(token)
                .username(u.getUsername())
                .rol(u.getRol().name())
                .expiresIn(jwtService.getExpirationMs())
                .sucursalId(u.getSucursal() != null ? u.getSucursal().getId() : null)
                .sucursalNombre(u.getSucursal() != null ? u.getSucursal().getNombre() : null)
                .build();
    }
}
