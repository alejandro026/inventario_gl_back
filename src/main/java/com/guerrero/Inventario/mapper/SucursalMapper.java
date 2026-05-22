package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.SucursalDTO;
import com.guerrero.Inventario.model.Sucursal;

public final class SucursalMapper {

    private SucursalMapper() {}

    public static SucursalDTO toDto(Sucursal s) {
        if (s == null) return null;
        return new SucursalDTO(s.getId(), s.getNombre(), s.getDireccion(), s.getTelefono());
    }

    public static Sucursal toEntity(SucursalDTO dto) {
        if (dto == null) return null;
        Sucursal s = new Sucursal();
        s.setId(dto.getId());
        s.setNombre(dto.getNombre());
        s.setDireccion(dto.getDireccion());
        s.setTelefono(dto.getTelefono());
        return s;
    }
}
