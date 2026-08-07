package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.CategoriaDTO;
import com.guerrero.Inventario.model.Categoria;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoriaMapperTest {

    @Test
    void toDto_null_devuelveNull() {
        assertThat(CategoriaMapper.toDto(null)).isNull();
    }

    @Test
    void toDto_mapeaCampos() {
        Categoria c = new Categoria();
        c.setId(1L);
        c.setNombre("PAPELERIA");
        c.setDescripcion("Articulos de oficina");

        CategoriaDTO dto = CategoriaMapper.toDto(c);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNombre()).isEqualTo("PAPELERIA");
        assertThat(dto.getDescripcion()).isEqualTo("Articulos de oficina");
    }

    @Test
    void toEntity_null_devuelveNull() {
        assertThat(CategoriaMapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_normalizaNombreAMayusculas() {
        CategoriaDTO dto = new CategoriaDTO(1L, "papeleria", "desc");

        Categoria c = CategoriaMapper.toEntity(dto);

        assertThat(c.getNombre()).isEqualTo("PAPELERIA");
    }

    @Test
    void toEntity_nombreNull_noLanzaExcepcion() {
        CategoriaDTO dto = new CategoriaDTO(1L, null, "desc");

        Categoria c = CategoriaMapper.toEntity(dto);

        assertThat(c.getNombre()).isNull();
    }
}
