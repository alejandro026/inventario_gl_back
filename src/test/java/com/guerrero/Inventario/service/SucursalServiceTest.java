package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.SucursalDTO;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.Sucursal;
import com.guerrero.Inventario.repository.SucursalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SucursalServiceTest {

    @Mock
    private SucursalRepository sucursalRepository;

    @InjectMocks
    private SucursalService service;

    private Sucursal sucursal() {
        Sucursal s = new Sucursal();
        s.setId(1L);
        s.setNombre("Centro");
        s.setDireccion("Av. Reforma 123");
        s.setTelefono("5512345678");
        return s;
    }

    @Test
    void listar_mapeaLista() {
        when(sucursalRepository.findAll()).thenReturn(List.of(sucursal()));

        List<SucursalDTO> resultado = service.listar();

        assertThat(resultado).hasSize(1);
    }

    @Test
    void buscar_noExistente_lanzaResourceNotFound() {
        when(sucursalRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_nombreDuplicado_lanzaDuplicateResourceException() {
        SucursalDTO dto = new SucursalDTO(null, "Centro", "Direccion", null);
        when(sucursalRepository.existsByNombreIgnoreCase("Centro")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(dto)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void crear_exitoso() {
        SucursalDTO dto = new SucursalDTO(null, "Norte", "Direccion Norte", "555");
        when(sucursalRepository.existsByNombreIgnoreCase("Norte")).thenReturn(false);
        when(sucursalRepository.save(any(Sucursal.class))).thenAnswer(inv -> inv.getArgument(0));

        SucursalDTO creada = service.crear(dto);

        assertThat(creada.getNombre()).isEqualTo("Norte");
    }

    @Test
    void actualizar_nombreDuplicadoOtraSucursal_lanzaExcepcion() {
        Sucursal existente = sucursal();
        Sucursal otra = new Sucursal();
        otra.setId(2L);
        otra.setNombre("Norte");

        SucursalDTO dto = new SucursalDTO(null, "Norte", "Direccion", null);
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(sucursalRepository.findByNombreIgnoreCase("Norte")).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> service.actualizar(1L, dto)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void actualizar_exitoso() {
        Sucursal existente = sucursal();
        SucursalDTO dto = new SucursalDTO(null, "Centro Renovado", "Nueva Direccion", "999");
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(sucursalRepository.findByNombreIgnoreCase("Centro Renovado")).thenReturn(Optional.empty());
        when(sucursalRepository.save(any(Sucursal.class))).thenAnswer(inv -> inv.getArgument(0));

        SucursalDTO actualizada = service.actualizar(1L, dto);

        assertThat(actualizada.getNombre()).isEqualTo("Centro Renovado");
        assertThat(actualizada.getTelefono()).isEqualTo("999");
    }

    @Test
    void eliminar_existente_llamaDelete() {
        Sucursal existente = sucursal();
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        verify(sucursalRepository).delete(existente);
    }
}
