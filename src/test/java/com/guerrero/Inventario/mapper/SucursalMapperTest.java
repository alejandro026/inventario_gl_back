package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.SucursalDTO;
import com.guerrero.Inventario.model.Sucursal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SucursalMapperTest {

    @Test
    void toDto_null_devuelveNull() {
        assertThat(SucursalMapper.toDto(null)).isNull();
    }

    @Test
    void toDto_mapeaCampos() {
        Sucursal s = new Sucursal();
        s.setId(1L);
        s.setNombre("Centro");
        s.setDireccion("Av. Reforma 123");
        s.setTelefono("5512345678");

        SucursalDTO dto = SucursalMapper.toDto(s);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNombre()).isEqualTo("Centro");
        assertThat(dto.getDireccion()).isEqualTo("Av. Reforma 123");
        assertThat(dto.getTelefono()).isEqualTo("5512345678");
    }

    @Test
    void toEntity_null_devuelveNull() {
        assertThat(SucursalMapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_mapeaCampos() {
        SucursalDTO dto = new SucursalDTO(2L, "Norte", "Direccion Norte", "555");

        Sucursal s = SucursalMapper.toEntity(dto);

        assertThat(s.getId()).isEqualTo(2L);
        assertThat(s.getNombre()).isEqualTo("Norte");
    }
}
