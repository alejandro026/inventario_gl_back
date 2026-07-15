package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.CajaMovimientoDTO;
import com.guerrero.Inventario.dto.CajaTurnoDTO;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.*;
import com.guerrero.Inventario.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CajaService {

    private final CajaTurnoRepository cajaTurnoRepository;
    private final CajaMovimientoRepository cajaMovimientoRepository;
    private final VentaRepository ventaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;

    public CajaService(CajaTurnoRepository cajaTurnoRepository,
                       CajaMovimientoRepository cajaMovimientoRepository,
                       VentaRepository ventaRepository,
                       SucursalRepository sucursalRepository,
                       UsuarioRepository usuarioRepository) {
        this.cajaTurnoRepository = cajaTurnoRepository;
        this.cajaMovimientoRepository = cajaMovimientoRepository;
        this.ventaRepository = ventaRepository;
        this.sucursalRepository = sucursalRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public CajaTurnoDTO obtenerEstadoActual(Long sucursalId, Long usuarioId) {
        return cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(sucursalId, usuarioId, "ABIERTO")
                .map(this::toDto)
                .orElse(null);
    }

    public CajaTurnoDTO apertura(Long sucursalId, Long usuarioId, Double montoApertura) {
        cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(sucursalId, usuarioId, "ABIERTO")
                .ifPresent(t -> {
                    throw new IllegalStateException("Ya existe un turno de caja abierto para este usuario en esta sucursal");
                });

        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal", sucursalId));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        CajaTurno turno = new CajaTurno();
        turno.setSucursal(sucursal);
        turno.setUsuario(usuario);
        turno.setFechaApertura(LocalDateTime.now());
        turno.setMontoApertura(montoApertura != null ? montoApertura : 0.0);
        turno.setMontoCierreTeorico(turno.getMontoApertura());
        turno.setMontoCierreReal(0.0);
        turno.setDiferencia(0.0);
        turno.setEstado("ABIERTO");

        return toDto(cajaTurnoRepository.save(turno));
    }

    public CajaMovimientoDTO registrarMovimiento(Long turnoId, String tipo, Double monto, String concepto) {
        CajaTurno turno = cajaTurnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("CajaTurno", turnoId));

        if (!"ABIERTO".equals(turno.getEstado())) {
            throw new IllegalStateException("No se pueden registrar movimientos en un turno cerrado");
        }

        CajaMovimiento movimiento = new CajaMovimiento();
        movimiento.setCajaTurno(turno);
        movimiento.setTipo(tipo.toUpperCase());
        movimiento.setMonto(monto);
        movimiento.setConcepto(concepto);
        movimiento.setFecha(LocalDateTime.now());

        CajaMovimiento saved = cajaMovimientoRepository.save(movimiento);
        
        // Update theoretical amount
        actualizarMontoTeorico(turno);

        return toMovimientoDto(saved);
    }

    public CajaTurnoDTO cierre(Long turnoId, Double montoCierreReal, String notas) {
        CajaTurno turno = cajaTurnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResourceNotFoundException("CajaTurno", turnoId));

        if (!"ABIERTO".equals(turno.getEstado())) {
            throw new IllegalStateException("El turno ya está cerrado");
        }

        turno.setFechaCierre(LocalDateTime.now());
        actualizarMontoTeorico(turno);

        turno.setMontoCierreReal(montoCierreReal != null ? montoCierreReal : 0.0);
        turno.setDiferencia(turno.getMontoCierreReal() - turno.getMontoCierreTeorico());
        turno.setEstado("CERRADO");
        turno.setNotas(notas);

        return toDto(cajaTurnoRepository.save(turno));
    }

    @Transactional(readOnly = true)
    public List<CajaTurnoDTO> listarHistorial(Long sucursalId) {
        return cajaTurnoRepository.findBySucursalIdOrderByFechaAperturaDesc(sucursalId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CajaMovimientoDTO> listarMovimientos(Long turnoId) {
        return cajaMovimientoRepository.findByCajaTurnoIdOrderByFechaAsc(turnoId).stream()
                .map(this::toMovimientoDto)
                .collect(Collectors.toList());
    }

    private void actualizarMontoTeorico(CajaTurno turno) {
        LocalDateTime fin = turno.getFechaCierre() != null ? turno.getFechaCierre() : LocalDateTime.now();
        
        List<Venta> ventas = ventaRepository.findSalesInTurn(
                turno.getSucursal().getId(),
                turno.getUsuario().getId(),
                turno.getFechaApertura(),
                fin
        );

        double totalEfectivoVentas = ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA && v.getMetodoPago() == Venta.MetodoPago.EFECTIVO)
                .mapToDouble(Venta::getTotal)
                .sum();

        List<CajaMovimiento> movimientos = cajaMovimientoRepository.findByCajaTurnoIdOrderByFechaAsc(turno.getId());
        double ingresos = movimientos.stream().filter(m -> "INGRESO".equals(m.getTipo())).mapToDouble(CajaMovimiento::getMonto).sum();
        double egresos = movimientos.stream().filter(m -> "EGRESO".equals(m.getTipo())).mapToDouble(CajaMovimiento::getMonto).sum();

        turno.setMontoCierreTeorico(turno.getMontoApertura() + totalEfectivoVentas + ingresos - egresos);
    }

    private CajaTurnoDTO toDto(CajaTurno t) {
        if (t == null) return null;
        CajaTurnoDTO dto = new CajaTurnoDTO();
        dto.setId(t.getId());
        dto.setIdSucursal(t.getSucursal().getId());
        dto.setSucursalNombre(t.getSucursal().getNombre());
        dto.setIdUsuario(t.getUsuario().getId());
        dto.setUsuarioNombre(t.getUsuario().getNombre());
        dto.setFechaApertura(t.getFechaApertura());
        dto.setFechaCierre(t.getFechaCierre());
        dto.setMontoApertura(t.getMontoApertura());
        dto.setMontoCierreReal(t.getMontoCierreReal() != null ? t.getMontoCierreReal() : 0.0);
        dto.setEstado(t.getEstado());
        dto.setNotas(t.getNotas());

        // Calcular desgloses para reportes
        LocalDateTime fin = t.getFechaCierre() != null ? t.getFechaCierre() : LocalDateTime.now();
        List<Venta> ventas = ventaRepository.findSalesInTurn(
                t.getSucursal().getId(),
                t.getUsuario().getId(),
                t.getFechaApertura(),
                fin
        );

        // Calcular efectivo teórico de forma dinámica y en tiempo real
        double totalEfectivoVentas = ventas.stream()
                .filter(v -> v.getEstado() == Venta.EstadoVenta.COMPLETADA && v.getMetodoPago() == Venta.MetodoPago.EFECTIVO)
                .mapToDouble(Venta::getTotal)
                .sum();

        List<CajaMovimiento> movimientos = cajaMovimientoRepository.findByCajaTurnoIdOrderByFechaAsc(t.getId());
        double ingresos = movimientos.stream().filter(m -> "INGRESO".equals(m.getTipo())).mapToDouble(CajaMovimiento::getMonto).sum();
        double egresos = movimientos.stream().filter(m -> "EGRESO".equals(m.getTipo())).mapToDouble(CajaMovimiento::getMonto).sum();

        double teorico = t.getMontoApertura() + totalEfectivoVentas + ingresos - egresos;
        dto.setMontoCierreTeorico(teorico);

        if ("CERRADO".equals(t.getEstado())) {
            dto.setDiferencia(dto.getMontoCierreReal() - teorico);
        } else {
            dto.setDiferencia(0.0);
        }

        java.util.Map<String, Double> metodosMap = new java.util.HashMap<>();
        metodosMap.put("EFECTIVO", 0.0);
        metodosMap.put("TARJETA", 0.0);
        metodosMap.put("TRANSFERENCIA", 0.0);
        metodosMap.put("CREDITO", 0.0);

        for (Venta v : ventas) {
            if (v.getEstado() == Venta.EstadoVenta.COMPLETADA) {
                String met = v.getMetodoPago().name();
                metodosMap.put(met, metodosMap.getOrDefault(met, 0.0) + v.getTotal());
            }
        }
        dto.setDesgloseMetodosPago(metodosMap);

        java.util.Map<String, Double> categoriasMap = new java.util.HashMap<>();
        for (Venta v : ventas) {
            if (v.getEstado() == Venta.EstadoVenta.COMPLETADA && v.getMetodoPago() == Venta.MetodoPago.EFECTIVO) {
                for (DetalleVenta d : v.getDetalle()) {
                    String catName = "Sin Categoría";
                    if (d.getProducto() != null && d.getProducto().getCategoria() != null) {
                        catName = d.getProducto().getCategoria().getNombre();
                    }
                    double sub = d.getSubtotal() != null ? d.getSubtotal() : 0.0;
                    categoriasMap.put(catName, categoriasMap.getOrDefault(catName, 0.0) + sub);
                }
            }
        }
        dto.setDesgloseCategorias(categoriasMap);

        return dto;
    }

    private CajaMovimientoDTO toMovimientoDto(CajaMovimiento m) {
        if (m == null) return null;
        return new CajaMovimientoDTO(
                m.getId(),
                m.getCajaTurno().getId(),
                m.getTipo(),
                m.getMonto(),
                m.getConcepto(),
                m.getFecha()
        );
    }
}
