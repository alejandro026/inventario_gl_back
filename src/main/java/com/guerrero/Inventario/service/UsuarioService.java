package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.UsuarioDTO;
import com.guerrero.Inventario.dto.UsuarioSaveRequest;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    public UsuarioDTO crear(UsuarioSaveRequest req) {
        if (req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            throw new BusinessException("La contraseña es obligatoria para la creación del usuario");
        }
        if (req.getPassword().trim().length() < 6) {
            throw new BusinessException("La contraseña debe tener al menos 6 caracteres");
        }
        if (usuarioRepository.existsByUsername(req.getUsername())) {
            throw new DuplicateResourceException("El username '" + req.getUsername() + "' ya está registrado");
        }
        if (usuarioRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("El email '" + req.getEmail() + "' ya está registrado");
        }

        Usuario u = new Usuario();
        u.setNombre(req.getNombre());
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setRol(req.getRol());
        u.setActivo(req.getActivo() != null ? req.getActivo() : Boolean.TRUE);

        Usuario guardado = usuarioRepository.save(u);
        return mapearADTO(guardado);
    }

    public UsuarioDTO actualizar(Long id, UsuarioSaveRequest req) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        usuarioRepository.findByUsername(req.getUsername()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new DuplicateResourceException("El username '" + req.getUsername() + "' ya está registrado por otro usuario");
            }
        });

        usuarioRepository.findByEmail(req.getEmail()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new DuplicateResourceException("El email '" + req.getEmail() + "' ya está registrado por otro usuario");
            }
        });

        u.setNombre(req.getNombre());
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setRol(req.getRol());
        u.setActivo(req.getActivo());

        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            if (req.getPassword().trim().length() < 6) {
                throw new BusinessException("La contraseña debe tener al menos 6 caracteres");
            }
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        Usuario guardado = usuarioRepository.save(u);
        return mapearADTO(guardado);
    }

    public void cambiarPassword(Long id, String nuevaPassword) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        if (nuevaPassword == null || nuevaPassword.trim().isEmpty()) {
            throw new BusinessException("La contraseña no puede estar vacía");
        }

        u.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(u);
    }

    private UsuarioDTO mapearADTO(Usuario u) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(u.getId());
        dto.setNombre(u.getNombre());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setRol(u.getRol());
        dto.setActivo(u.getActivo());
        return dto;
    }
}
