package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.MovimientoInventarioDTO;
import com.guerrero.Inventario.service.KardexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kardex")
@Tag(name = "Kardex", description = "Movimientos de Inventario (Kardex)")
public class KardexController {

    private final KardexService service;

    public KardexController(KardexService service) {
        this.service = service;
    }

    @GetMapping("/producto/{productoId}/sucursal/{sucursalId}")
    @Operation(summary = "Obtener el historial de movimientos de inventario de un producto en una sucursal")
    public ResponseEntity<List<MovimientoInventarioDTO>> listarPorProductoAndSucursal(@PathVariable Long productoId,
                                                                                       @PathVariable Long sucursalId) {
        return ResponseEntity.ok(service.listarPorProductoAndSucursal(productoId, sucursalId));
    }

    @GetMapping("/producto/{productoId}")
    @Operation(summary = "Obtener el historial global de movimientos de un producto")
    public ResponseEntity<List<MovimientoInventarioDTO>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.listarPorProducto(productoId));
    }
}
