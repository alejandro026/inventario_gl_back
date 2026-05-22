package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.ProductoDTO;
import com.guerrero.Inventario.model.Categoria;
import com.guerrero.Inventario.model.Producto;

public final class ProductoMapper {

    private ProductoMapper() {}

    public static ProductoDTO toDto(Producto p) {
        if (p == null) return null;
        ProductoDTO dto = new ProductoDTO();
        dto.setId(p.getId());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setPrecio(p.getPrecio());
        dto.setCantidad(p.getCantidad());
        dto.setStockMinimo(p.getStockMinimo());
        dto.setActivo(p.getActivo());
        if (p.getCategoria() != null) {
            dto.setCategoriaId(p.getCategoria().getId());
            dto.setCategoriaNombre(p.getCategoria().getNombre());
        }
        return dto;
    }

    public static Producto toEntity(ProductoDTO dto, Categoria categoria) {
        if (dto == null) return null;
        Producto p = new Producto();
        p.setId(dto.getId());
        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        p.setPrecio(dto.getPrecio());
        p.setCantidad(dto.getCantidad());
        p.setStockMinimo(dto.getStockMinimo());
        p.setActivo(dto.getActivo() == null ? Boolean.TRUE : dto.getActivo());
        p.setCategoria(categoria);
        return p;
    }
}
