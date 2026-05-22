package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.CategoriaDTO;
import com.guerrero.Inventario.model.Categoria;

public final class CategoriaMapper {

    private CategoriaMapper() {}

    public static CategoriaDTO toDto(Categoria c) {
        if (c == null) return null;
        return new CategoriaDTO(c.getId(), c.getNombre(), c.getDescripcion());
    }

    public static Categoria toEntity(CategoriaDTO dto) {
        if (dto == null) return null;
        Categoria c = new Categoria();
        c.setId(dto.getId());
        c.setNombre(dto.getNombre() == null ? null : dto.getNombre().toUpperCase());
        c.setDescripcion(dto.getDescripcion());
        return c;
    }
}
