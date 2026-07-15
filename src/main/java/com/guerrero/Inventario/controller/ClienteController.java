package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.AbonoCreditoDTO;
import com.guerrero.Inventario.dto.ClienteDTO;
import com.guerrero.Inventario.dto.CuentaPorCobrarDTO;
import com.guerrero.Inventario.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Gestion de clientes y cuentas por cobrar (credito)")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos los clientes o buscar por nombre")
    public List<ClienteDTO> listar(@RequestParam(required = false) String query) {
        if (query != null && !query.trim().isEmpty()) {
            return service.buscarPorNombre(query);
        }
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por id")
    public ClienteDTO obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo cliente")
    public ResponseEntity<ClienteDTO> crear(@Valid @RequestBody ClienteDTO dto) {
        ClienteDTO creada = service.crear(dto);
        return ResponseEntity.created(URI.create("/api/clientes/" + creada.getId())).body(creada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente")
    public ClienteDTO actualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto) {
        return service.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cliente")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/cuentas-cobrar")
    @Operation(summary = "Obtener deudas (cuentas por cobrar) de un cliente")
    public List<CuentaPorCobrarDTO> obtenerCuentasPorCobrar(@PathVariable Long id) {
        return service.obtenerCuentasPorCobrar(id);
    }

    @PostMapping("/cuentas/{cuentaId}/abono")
    @Operation(summary = "Registrar un abono a una cuenta por cobrar")
    public ResponseEntity<AbonoCreditoDTO> registrarAbono(@PathVariable Long cuentaId,
                                                           @RequestParam Double monto,
                                                           @RequestParam(required = false, defaultValue = "EFECTIVO") String metodoPago) {
        return ResponseEntity.ok(service.registrarAbono(cuentaId, monto, metodoPago));
    }

    @GetMapping("/cuentas/{cuentaId}/abonos")
    @Operation(summary = "Obtener el historial de abonos a una cuenta por cobrar")
    public List<AbonoCreditoDTO> obtenerHistorialAbonos(@PathVariable Long cuentaId) {
        return service.obtenerHistorialAbonos(cuentaId);
    }
}
