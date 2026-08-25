package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.model.Promocion;
import com.guerrero.Inventario.service.PromocionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/promociones")
@Tag(name = "Promociones", description = "Gestión de campañas de descuentos y promociones temporales")
public class PromocionController {

    private final PromocionService service;

    public PromocionController(PromocionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas las promociones configuradas (Solo ADMIN)")
    public List<Promocion> listar() {
        return service.listarTodas();
    }

    @GetMapping("/activa")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    @Operation(summary = "Obtener la promoción activa vigente (ADMIN y EMPLEADO)")
    public ResponseEntity<Promocion> obtenerActiva() {
        Promocion activa = service.obtenerActiva(LocalDateTime.now());
        if (activa == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(activa);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener promoción por ID (Solo ADMIN)")
    public Promocion obtenerPorId(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una nueva promoción (Solo ADMIN)")
    public Promocion crear(@RequestBody Promocion promocion) {
        // Asegurarse de que el ID sea nulo para la creación
        promocion.setId(null);
        return service.guardar(promocion);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar una promoción existente (Solo ADMIN)")
    public Promocion actualizar(@PathVariable Long id, @RequestBody Promocion promocion) {
        promocion.setId(id);
        return service.guardar(promocion);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una promoción (Solo ADMIN)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
