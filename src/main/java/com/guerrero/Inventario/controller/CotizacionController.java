package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.CotizacionDTO;
import com.guerrero.Inventario.service.CotizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cotizaciones")
@Tag(name = "Cotizaciones", description = "Gestión de cotizaciones y presupuestos temporales")
public class CotizacionController {

    private final CotizacionService service;

    public CotizacionController(CotizacionService service) {
        this.service = service;
    }

    @GetMapping("/sucursal/{sucursalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    @Operation(summary = "Listar todas las cotizaciones de una sucursal")
    public List<CotizacionDTO> listarPorSucursal(@PathVariable Long sucursalId) {
        return service.listarPorSucursal(sucursalId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    @Operation(summary = "Obtener detalle de una cotización por ID")
    public CotizacionDTO obtenerPorId(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    @Operation(summary = "Crear una nueva cotización")
    public CotizacionDTO crear(@Valid @RequestBody CotizacionDTO dto) {
        dto.setId(null);
        return service.guardar(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLEADO')")
    @Operation(summary = "Actualizar una cotización existente (solo si está PENDIENTE)")
    public CotizacionDTO actualizar(@PathVariable Long id, @Valid @RequestBody CotizacionDTO dto) {
        dto.setId(id);
        return service.guardar(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una cotización (solo ADMIN)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
