package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.CategoriaDTO;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.Categoria;
import com.guerrero.Inventario.repository.CategoriaRepository;
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
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService service;

    private Categoria categoria() {
        Categoria c = new Categoria();
        c.setId(1L);
        c.setNombre("PAPELERIA");
        c.setDescripcion("Articulos de oficina");
        return c;
    }

    @Test
    void listar_mapeaLista() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria()));

        List<CategoriaDTO> resultado = service.listar();

        assertThat(resultado).hasSize(1);
    }

    @Test
    void obtener_noExistente_lanzaResourceNotFound() {
        when(categoriaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_nombreDuplicado_lanzaDuplicateResourceException() {
        CategoriaDTO dto = new CategoriaDTO(null, "papeleria", null);
        when(categoriaRepository.existsByNombreIgnoreCase("PAPELERIA")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(dto)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void crear_exitoso_normalizaNombreAMayusculas() {
        CategoriaDTO dto = new CategoriaDTO(null, "papeleria", "desc");
        when(categoriaRepository.existsByNombreIgnoreCase("PAPELERIA")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaDTO creada = service.crear(dto);

        assertThat(creada.getNombre()).isEqualTo("PAPELERIA");
    }

    @Test
    void actualizar_nombreDuplicadoEnOtraCategoria_lanzaExcepcion() {
        Categoria existente = categoria();
        Categoria otra = new Categoria();
        otra.setId(2L);
        otra.setNombre("ELECTRONICA");

        CategoriaDTO dto = new CategoriaDTO(null, "electronica", "desc");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findByNombreIgnoreCase("ELECTRONICA")).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> service.actualizar(1L, dto)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void actualizar_exitoso() {
        Categoria existente = categoria();
        CategoriaDTO dto = new CategoriaDTO(null, "papeleria escolar", "nueva desc");
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findByNombreIgnoreCase("PAPELERIA ESCOLAR")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaDTO actualizada = service.actualizar(1L, dto);

        assertThat(actualizada.getNombre()).isEqualTo("PAPELERIA ESCOLAR");
        assertThat(actualizada.getDescripcion()).isEqualTo("nueva desc");
    }

    @Test
    void eliminar_noExistente_lanzaResourceNotFound() {
        when(categoriaRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.eliminar(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_existente_llamaDeleteById() {
        when(categoriaRepository.existsById(1L)).thenReturn(true);

        service.eliminar(1L);

        verify(categoriaRepository).deleteById(1L);
    }
}
