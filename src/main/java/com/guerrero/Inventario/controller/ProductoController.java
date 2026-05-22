package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.ProductoDTO;
import com.guerrero.Inventario.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Gestion de productos del inventario")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar productos (paginado, filtro por categoria o nombre)")
    public Page<ProductoDTO> listar(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Filtrar por id de categoria") @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Buscar por parte del nombre") @RequestParam(required = false) String buscar) {
        return service.listar(pageable, categoriaId, buscar);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por id")
    public ProductoDTO obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/bajo-stock")
    @Operation(summary = "Productos cuya cantidad <= stock minimo")
    public List<ProductoDTO> bajoStock() {
        return service.productosBajoStock();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear producto (ADMIN)")
    public ResponseEntity<ProductoDTO> crear(@Valid @RequestBody ProductoDTO dto) {
        ProductoDTO creado = service.crear(dto);
        return ResponseEntity.created(URI.create("/api/productos/" + creado.getId())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar producto (ADMIN)")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoDTO dto) {
        return service.actualizar(id, dto);
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    @Operation(summary = "Ajustar stock sumando un delta (puede ser negativo)")
    public ProductoDTO ajustarStock(@PathVariable Long id,
                                    @Parameter(description = "Delta a aplicar (+/-)", example = "10")
                                    @RequestParam Integer delta) {
        return service.ajustarStock(id, delta);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar producto (ADMIN)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
