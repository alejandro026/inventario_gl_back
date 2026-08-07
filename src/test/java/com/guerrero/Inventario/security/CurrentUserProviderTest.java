package com.guerrero.Inventario.security;

import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CurrentUserProvider provider;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtenerONull_sinAutenticacion_devuelveNull() {
        SecurityContextHolder.clearContext();

        assertThat(provider.obtenerONull()).isNull();
    }

    @Test
    void obtenerONull_noAutenticado_devuelveNull() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user", "pass");
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(provider.obtenerONull()).isNull();
    }

    @Test
    void obtenerONull_principalEsUsuario_devuelveDirectamente() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("alejandro");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(usuario, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Usuario resultado = provider.obtenerONull();

        assertThat(resultado).isSameAs(usuario);
    }

    @Test
    void obtenerONull_principalEsUserDetails_buscaPorUsername() {
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setUsername("otro");

        UserDetails userDetails = User.withUsername("otro").password("x").authorities(List.of()).build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByUsername("otro")).thenReturn(Optional.of(usuario));

        Usuario resultado = provider.obtenerONull();

        assertThat(resultado).isSameAs(usuario);
    }

    @Test
    void obtenerONull_userDetailsNoEncontradoEnBD_devuelveNull() {
        UserDetails userDetails = User.withUsername("fantasma").password("x").authorities(List.of()).build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThat(provider.obtenerONull()).isNull();
    }

    @Test
    void obtenerOFallar_sinUsuario_lanzaAccessDeniedException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> provider.obtenerOFallar()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void obtenerOFallar_conUsuario_loDevuelve() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(usuario, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(provider.obtenerOFallar()).isSameAs(usuario);
    }
}
