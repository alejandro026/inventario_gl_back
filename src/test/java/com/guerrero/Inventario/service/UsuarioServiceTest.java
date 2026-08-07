package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.UsuarioDTO;
import com.guerrero.Inventario.dto.UsuarioSaveRequest;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.Rol;
import com.guerrero.Inventario.model.Sucursal;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.SucursalRepository;
import com.guerrero.Inventario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SucursalRepository sucursalRepository;

    @InjectMocks
    private UsuarioService service;

    private UsuarioSaveRequest reqValido() {
        UsuarioSaveRequest req = new UsuarioSaveRequest();
        req.setNombre("Juan Perez");
        req.setUsername("juan");
        req.setEmail("juan@test.com");
        req.setPassword("clave123");
        req.setRol(Rol.EMPLEADO);
        req.setActivo(true);
        return req;
    }

    private Usuario usuarioExistente() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombre("Juan Perez");
        u.setUsername("juan");
        u.setEmail("juan@test.com");
        u.setRol(Rol.EMPLEADO);
        u.setActivo(true);
        u.setPassword("hash-viejo");
        return u;
    }

    @Test
    void crear_passwordVacia_lanzaBusinessException() {
        UsuarioSaveRequest req = reqValido();
        req.setPassword("  ");

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(BusinessException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crear_passwordCorta_lanzaBusinessException() {
        UsuarioSaveRequest req = reqValido();
        req.setPassword("123");

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(BusinessException.class);
    }

    @Test
    void crear_usernameExistente_lanzaDuplicateResourceException() {
        UsuarioSaveRequest req = reqValido();
        when(usuarioRepository.existsByUsername("juan")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void crear_emailExistente_lanzaDuplicateResourceException() {
        UsuarioSaveRequest req = reqValido();
        when(usuarioRepository.existsByUsername("juan")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void crear_conSucursalInexistente_lanzaResourceNotFound() {
        UsuarioSaveRequest req = reqValido();
        req.setSucursalId(5L);
        when(usuarioRepository.existsByUsername("juan")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(sucursalRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(req)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_exitoso_conSucursal() {
        UsuarioSaveRequest req = reqValido();
        req.setSucursalId(5L);
        Sucursal sucursal = new Sucursal();
        sucursal.setId(5L);
        sucursal.setNombre("Centro");

        when(usuarioRepository.existsByUsername("juan")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(sucursalRepository.findById(5L)).thenReturn(Optional.of(sucursal));
        when(passwordEncoder.encode("clave123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO dto = service.crear(req);

        assertThat(dto.getUsername()).isEqualTo("juan");
        assertThat(dto.getSucursalId()).isEqualTo(5L);
        assertThat(dto.getSucursalNombre()).isEqualTo("Centro");
    }

    @Test
    void crear_exitoso_sinSucursal() {
        UsuarioSaveRequest req = reqValido();
        when(usuarioRepository.existsByUsername("juan")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO dto = service.crear(req);

        assertThat(dto.getSucursalId()).isNull();
    }

    @Test
    void actualizar_usernameDuplicadoOtroUsuario_lanzaDuplicateResourceException() {
        Usuario existente = usuarioExistente();
        Usuario otro = new Usuario();
        otro.setId(2L);

        UsuarioSaveRequest req = reqValido();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByUsername("juan")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.actualizar(1L, req)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void actualizar_sinCambiarPassword_conservaHashAnterior() {
        Usuario existente = usuarioExistente();
        UsuarioSaveRequest req = reqValido();
        req.setPassword(null);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByUsername("juan")).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(1L, req);

        assertThat(existente.getPassword()).isEqualTo("hash-viejo");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void actualizar_conPasswordNueva_laEncripta() {
        Usuario existente = usuarioExistente();
        UsuarioSaveRequest req = reqValido();
        req.setPassword("nuevaClave123");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByUsername("juan")).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(1L, req);

        assertThat(existente.getPassword()).isEqualTo("hash-nuevo");
    }

    @Test
    void actualizar_passwordNuevaCorta_lanzaBusinessException() {
        Usuario existente = usuarioExistente();
        UsuarioSaveRequest req = reqValido();
        req.setPassword("123");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByUsername("juan")).thenReturn(Optional.of(existente));
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.actualizar(1L, req)).isInstanceOf(BusinessException.class);
    }

    @Test
    void cambiarPassword_vacia_lanzaBusinessException() {
        Usuario existente = usuarioExistente();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.cambiarPassword(1L, " ")).isInstanceOf(BusinessException.class);
    }

    @Test
    void cambiarPassword_exitosa_encriptaYGuarda() {
        Usuario existente = usuarioExistente();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode("nueva123")).thenReturn("hash-nuevo");

        service.cambiarPassword(1L, "nueva123");

        assertThat(existente.getPassword()).isEqualTo("hash-nuevo");
        verify(usuarioRepository).save(existente);
    }
}
