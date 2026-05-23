package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.PasswordChangeRequest;
import com.guerrero.Inventario.dto.UsuarioDTO;
import com.guerrero.Inventario.dto.UsuarioSaveRequest;
import com.guerrero.Inventario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema (ADMIN)")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios del sistema (ADMIN)")
    public ResponseEntity<List<UsuarioDTO>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario en el sistema (ADMIN)")
    public ResponseEntity<UsuarioDTO> crear(@Valid @RequestBody UsuarioSaveRequest req) {
        UsuarioDTO creado = service.crear(req);
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.getId())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar datos generales de un usuario existente (ADMIN)")
    public ResponseEntity<UsuarioDTO> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioSaveRequest req) {
        UsuarioDTO actualizado = service.actualizar(id, req);
        return ResponseEntity.ok(actualizado);
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Cambiar/Restablecer contraseña de un usuario (ADMIN)")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeRequest req) {
        service.cambiarPassword(id, req.getPassword());
        return ResponseEntity.noContent().build();
    }
}
