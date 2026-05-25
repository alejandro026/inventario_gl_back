package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.DetalleVentaDTO;
import com.guerrero.Inventario.dto.VentaDTO;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.InsufficientStockException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.mapper.VentaMapper;
import com.guerrero.Inventario.model.*;
import com.guerrero.Inventario.repository.UsuarioRepository;
import com.guerrero.Inventario.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final UsuarioRepository usuarioRepository;

    public VentaService(VentaRepository ventaRepository,
                        ProductoService productoService,
                        SucursalService sucursalService,
                        UsuarioRepository usuarioRepository) {
        this.ventaRepository = ventaRepository;
        this.productoService = productoService;
        this.sucursalService = sucursalService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<VentaDTO> listar(Pageable pageable, Long sucursalId, LocalDateTime inicio, LocalDateTime fin) {
        Page<Venta> page;
        if (inicio != null && fin != null) {
            page = sucursalId == null
                    ? ventaRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin, pageable)
                    : ventaRepository.findBySucursal_IdAndFechaBetweenOrderByFechaDesc(sucursalId, inicio, fin, pageable);
        } else {
            page = sucursalId == null
                    ? ventaRepository.findAllByOrderByFechaDesc(pageable)
                    : ventaRepository.findBySucursal_IdOrderByFechaDesc(sucursalId, pageable);
        }
        return page.map(VentaMapper::toDto);
    }

    @Transactional(readOnly = true)
    public VentaDTO obtener(Long id) {
        return VentaMapper.toDto(buscar(id));
    }

    @Transactional(readOnly = true)
    public Double totalVendido(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || inicio.isAfter(fin)) {
            throw new BusinessException("Rango de fechas invalido");
        }
        Double total = ventaRepository.totalVendidoEntre(inicio, fin);
        return total == null ? 0.0 : total;
    }

    public VentaDTO registrar(VentaDTO dto) {
        Sucursal sucursal = sucursalService.buscar(dto.getIdSucursal());

        Venta venta = new Venta();
        venta.setSucursal(sucursal);
        venta.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDateTime.now());
        venta.setEstado(parseEstado(dto.getEstado(), Venta.EstadoVenta.COMPLETADA));
        venta.setUsuario(usuarioActual());

        double total = 0.0;
        List<DetalleVentaDTO> detalles = dto.getDetalle();
        if (detalles == null || detalles.isEmpty()) {
            throw new BusinessException("La venta debe contener al menos un detalle");
        }

        for (DetalleVentaDTO d : detalles) {
            Producto producto = productoService.buscar(d.getProductoId());
            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new BusinessException("El producto '" + producto.getNombre() + "' esta inactivo");
            }
            int cantidad = d.getCantProd();
            if (cantidad <= 0) {
                throw new BusinessException("La cantidad debe ser positiva para el producto "
                        + producto.getNombre());
            }
            if (venta.getEstado() == Venta.EstadoVenta.COMPLETADA
                    && producto.getCantidad() < cantidad) {
                throw new InsufficientStockException(
                        "Stock insuficiente para '" + producto.getNombre()
                                + "'. Disponible: " + producto.getCantidad()
                                + ", solicitado: " + cantidad);
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantProd(cantidad);
            detalle.setPrecio(producto.getPrecio());
            detalle.setPrecioCompra(producto.getPrecioCompra() != null ? producto.getPrecioCompra() : 0.0);
            double subtotal = producto.getPrecio() * cantidad;
            detalle.setSubtotal(subtotal);
            venta.getDetalle().add(detalle);
            total += subtotal;

            if (venta.getEstado() == Venta.EstadoVenta.COMPLETADA) {
                producto.setCantidad(producto.getCantidad() - cantidad);
            }
        }

        venta.setTotal(total);
        Venta guardada = ventaRepository.save(venta);
        return VentaMapper.toDto(guardada);
    }

    public VentaDTO cancelar(Long id) {
        Venta v = buscar(id);
        if (v.getEstado() == Venta.EstadoVenta.CANCELADA) {
            throw new BusinessException("La venta ya esta cancelada");
        }
        // Reintegrar el stock si la venta estaba completada
        if (v.getEstado() == Venta.EstadoVenta.COMPLETADA) {
            for (DetalleVenta d : v.getDetalle()) {
                Producto p = d.getProducto();
                p.setCantidad(p.getCantidad() + d.getCantProd());
            }
        }
        v.setEstado(Venta.EstadoVenta.CANCELADA);
        return VentaMapper.toDto(ventaRepository.save(v));
    }

    public Venta buscar(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", id));
    }

    private Venta.EstadoVenta parseEstado(String raw, Venta.EstadoVenta porDefecto) {
        if (raw == null || raw.isBlank()) return porDefecto;
        try {
            return Venta.EstadoVenta.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Estado de venta invalido: " + raw);
        }
    }

    private Usuario usuarioActual() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof Usuario u) {
                return u;
            }
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                return usuarioRepository.findByUsername(ud.getUsername()).orElse(null);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
