package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.DetalleVentaDTO;
import com.guerrero.Inventario.dto.VentaDTO;
import com.guerrero.Inventario.model.DetalleVenta;
import com.guerrero.Inventario.model.Venta;

import java.util.List;
import java.util.stream.Collectors;

public final class VentaMapper {

    private VentaMapper() {}

    public static DetalleVentaDTO toDto(DetalleVenta d) {
        if (d == null) return null;
        DetalleVentaDTO dto = new DetalleVentaDTO();
        dto.setId(d.getId());
        dto.setCantProd(d.getCantProd());
        dto.setPrecio(d.getPrecio());
        dto.setPrecioCompra(d.getPrecioCompra() != null ? d.getPrecioCompra() : (d.getProducto() != null && d.getProducto().getPrecioCompra() != null ? d.getProducto().getPrecioCompra() : 0.0));
        dto.setSubtotal(d.getSubtotal());
        if (d.getProducto() != null) {
            dto.setProductoId(d.getProducto().getId());
            dto.setNombreProd(d.getProducto().getNombre());
            if (d.getProducto().getCategoria() != null) {
                dto.setCategoriaProd(d.getProducto().getCategoria().getNombre());
            }
        }
        return dto;
    }

    public static VentaDTO toDto(Venta v) {
        if (v == null) return null;
        VentaDTO dto = new VentaDTO();
        dto.setId(v.getId());
        dto.setFecha(v.getFecha());
        dto.setEstado(v.getEstado() == null ? null : v.getEstado().name());
        dto.setTotal(v.getTotal());
        if (v.getSucursal() != null) {
            dto.setIdSucursal(v.getSucursal().getId());
        }
        if (v.getUsuario() != null) {
            dto.setUsuarioNombre(v.getUsuario().getNombre());
        }
        List<DetalleVentaDTO> detalles = v.getDetalle() == null ? List.of() :
                v.getDetalle().stream().map(VentaMapper::toDto).collect(Collectors.toList());
        dto.setDetalle(detalles);
        return dto;
    }
}
