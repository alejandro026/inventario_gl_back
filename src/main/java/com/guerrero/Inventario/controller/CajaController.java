package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.CajaMovimientoDTO;
import com.guerrero.Inventario.dto.CajaTurnoDTO;
import com.guerrero.Inventario.service.CajaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/caja")
@Validated
@PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
@Tag(name = "Caja", description = "Control y Arqueo de Caja (Turnos)")
public class CajaController {

    private final CajaService service;

    public CajaController(CajaService service) {
        this.service = service;
    }

    @GetMapping("/estado-actual")
    @Operation(summary = "Obtener el turno de caja abierto actual del usuario autenticado en una sucursal " +
            "(ADMIN puede consultar el de otro usuario indicando usuarioId)")
    public ResponseEntity<CajaTurnoDTO> obtenerEstadoActual(@RequestParam Long sucursalId,
                                                             @RequestParam(required = false) Long usuarioId) {
        CajaTurnoDTO dto = service.obtenerEstadoActual(sucursalId, usuarioId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/apertura")
    @Operation(summary = "Abrir un nuevo turno de caja para el usuario autenticado " +
            "(ADMIN puede abrirlo para otro usuario indicando usuarioId)")
    public ResponseEntity<CajaTurnoDTO> apertura(@RequestParam Long sucursalId,
                                                 @RequestParam(required = false) Long usuarioId,
                                                 @RequestParam(required = false, defaultValue = "0.0") @PositiveOrZero BigDecimal montoApertura) {
        return ResponseEntity.ok(service.apertura(sucursalId, usuarioId, montoApertura));
    }

    @PostMapping("/movimiento")
    @Operation(summary = "Registrar un ingreso o egreso de caja")
    public ResponseEntity<CajaMovimientoDTO> registrarMovimiento(@RequestParam Long turnoId,
                                                                 @RequestParam String tipo,
                                                                 @RequestParam @Positive BigDecimal monto,
                                                                 @RequestParam String concepto) {
        return ResponseEntity.ok(service.registrarMovimiento(turnoId, tipo, monto, concepto));
    }

    @PostMapping("/cierre")
    @Operation(summary = "Cerrar un turno de caja (Arqueo)")
    public ResponseEntity<CajaTurnoDTO> cierre(@RequestParam Long turnoId,
                                               @RequestParam @PositiveOrZero BigDecimal montoCierreReal,
                                               @RequestParam(required = false) String notas) {
        return ResponseEntity.ok(service.cierre(turnoId, montoCierreReal, notas));
    }

    @GetMapping("/historial")
    @Operation(summary = "Obtener el historial de turnos de caja de una sucursal")
    public ResponseEntity<List<CajaTurnoDTO>> listarHistorial(@RequestParam Long sucursalId) {
        return ResponseEntity.ok(service.listarHistorial(sucursalId));
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Obtener los movimientos de un turno de caja")
    public ResponseEntity<List<CajaMovimientoDTO>> listarMovimientos(@RequestParam Long turnoId) {
        return ResponseEntity.ok(service.listarMovimientos(turnoId));
    }
}
