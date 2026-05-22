package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.VentaDTO;
import com.guerrero.Inventario.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Registro y consulta de ventas")
public class VentaController {

    private final VentaService service;

    public VentaController(VentaService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    @Operation(summary = "Listar ventas (paginado)")
    public Page<VentaDTO> listar(
            @ParameterObject @PageableDefault(size = 20, sort = "fecha") Pageable pageable,
            @Parameter(description = "Filtrar por id de sucursal") @RequestParam(required = false) Long sucursalId) {
        return service.listar(pageable, sucursalId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    @Operation(summary = "Obtener venta por id")
    public VentaDTO obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    @Operation(summary = "Registrar nueva venta y descontar stock")
    public ResponseEntity<VentaDTO> registrar(@Valid @RequestBody VentaDTO dto) {
        VentaDTO creada = service.registrar(dto);
        return ResponseEntity.created(URI.create("/api/ventas/" + creada.getId())).body(creada);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancelar venta y reintegrar stock (ADMIN)")
    public VentaDTO cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }

    @GetMapping("/reporte/total")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Total vendido (estado COMPLETADA) en un rango de fechas")
    public Map<String, Object> totalVendido(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        double total = service.totalVendido(inicio, fin);
        return Map.of("inicio", inicio, "fin", fin, "total", total);
    }
}
