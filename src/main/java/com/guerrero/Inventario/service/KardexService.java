package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.MovimientoInventarioDTO;
import com.guerrero.Inventario.model.MovimientoInventario;
import com.guerrero.Inventario.model.Producto;
import com.guerrero.Inventario.model.Sucursal;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.MovimientoInventarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class KardexService {

    private final MovimientoInventarioRepository repository;

    public KardexService(MovimientoInventarioRepository repository) {
        this.repository = repository;
    }

    public void registrarMovimiento(Producto producto, Sucursal sucursal, String tipo, Integer cantidad,
                                   String motivo, Usuario usuario, Long referenciaId) {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setProducto(producto);
        mov.setSucursal(sucursal);
        mov.setTipo(tipo.toUpperCase()); // ENTRADA, SALIDA
        mov.setCantidad(cantidad);
        mov.setMotivo(motivo.toUpperCase()); // VENTA, COMPRA, AJUSTE_MANUAL, CANCELACION
        mov.setFecha(LocalDateTime.now());
        mov.setUsuario(usuario);
        mov.setReferenciaId(referenciaId);

        repository.save(mov);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> listarPorProductoAndSucursal(Long productoId, Long sucursalId) {
        return repository.findByProductoIdAndSucursalIdOrderByFechaDesc(productoId, sucursalId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventarioDTO> listarPorProducto(Long productoId) {
        return repository.findByProductoIdOrderByFechaDesc(productoId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private MovimientoInventarioDTO toDto(MovimientoInventario m) {
        if (m == null) return null;
        MovimientoInventarioDTO dto = new MovimientoInventarioDTO();
        dto.setId(m.getId());
        dto.setIdProducto(m.getProducto().getId());
        dto.setProductoNombre(m.getProducto().getNombre());
        dto.setProductoCodigo(m.getProducto().getCodigo());
        dto.setIdSucursal(m.getSucursal().getId());
        dto.setTipo(m.getTipo());
        dto.setCantidad(m.getCantidad());
        dto.setMotivo(m.getMotivo());
        dto.setFecha(m.getFecha());
        if (m.getUsuario() != null) {
            dto.setUsuarioNombre(m.getUsuario().getNombre());
        }
        dto.setReferenciaId(m.getReferenciaId());
        return dto;
    }
}
