package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.CotizacionDTO;
import com.guerrero.Inventario.dto.DetalleCotizacionDTO;
import com.guerrero.Inventario.model.Cotizacion;
import com.guerrero.Inventario.model.DetalleCotizacion;
import java.util.stream.Collectors;

public class CotizacionMapper {

    public static CotizacionDTO toDto(Cotizacion c) {
        if (c == null) return null;
        CotizacionDTO dto = new CotizacionDTO();
        dto.setId(c.getId());
        dto.setFecha(c.getFecha());
        dto.setFechaVencimiento(c.getFechaVencimiento());
        dto.setSubtotal(c.getSubtotal());
        dto.setDescuento(c.getDescuento());
        dto.setTotal(c.getTotal());
        dto.setEstado(c.getEstado() != null ? c.getEstado().name() : null);
        dto.setVentaId(c.getVentaId());

        if (c.getSucursal() != null) {
            dto.setIdSucursal(c.getSucursal().getId());
            dto.setSucursalNombre(c.getSucursal().getNombre());
        }
        if (c.getUsuario() != null) {
            dto.setUsuarioNombre(c.getUsuario().getUsername());
        }
        if (c.getCliente() != null) {
            dto.setIdCliente(c.getCliente().getId());
            dto.setClienteNombre(c.getCliente().getNombre());
        }

        if (c.getDetalle() != null) {
            dto.setDetalle(c.getDetalle().stream()
                    .map(CotizacionMapper::toDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public static DetalleCotizacionDTO toDto(DetalleCotizacion d) {
        if (d == null) return null;
        DetalleCotizacionDTO dto = new DetalleCotizacionDTO();
        dto.setId(d.getId());
        dto.setCantProd(d.getCantProd());
        dto.setPrecio(d.getPrecio());
        dto.setSubtotal(d.getSubtotal());

        if (d.getProducto() != null) {
            dto.setProductoId(d.getProducto().getId());
            dto.setNombreProd(d.getProducto().getNombre());
            if (d.getProducto().getCategoria() != null) {
                dto.setCategoriaId(d.getProducto().getCategoria().getId());
                dto.setCategoriaNombre(d.getProducto().getCategoria().getNombre());
            }
        }
        return dto;
    }
}
