package com.guerrero.Inventario.security;

import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserProvider(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Devuelve el usuario autenticado actual, o null si no hay uno resoluble. */
    public Usuario obtenerONull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        if (principal instanceof UserDetails userDetails) {
            return usuarioRepository.findByUsername(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    /** Igual que {@link #obtenerONull()} pero lanza 403 si no hay usuario autenticado resoluble. */
    public Usuario obtenerOFallar() {
        Usuario usuario = obtenerONull();
        if (usuario == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No se pudo determinar el usuario autenticado");
        }
        return usuario;
    }
}
