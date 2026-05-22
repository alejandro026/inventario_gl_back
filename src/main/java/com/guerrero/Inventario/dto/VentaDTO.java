package com.guerrero.Inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Venta realizada en una sucursal")
public class VentaDTO {

    private Long id;

    @Schema(description = "Fecha (asignada por backend si viene null)", example = "2026-05-20T10:30:00")
    private LocalDateTime fecha;

    @Schema(description = "Estado (PENDIENTE / COMPLETADA / CANCELADA). Default: COMPLETADA",
            example = "COMPLETADA")
    private String estado;

    @NotNull(message = "La sucursal es obligatoria")
    @Schema(example = "1")
    private Long idSucursal;

    @Schema(description = "Nombre del usuario que registro la venta (solo respuesta)")
    private String usuarioNombre;

    @NotEmpty(message = "La venta debe tener al menos un detalle")
    @Valid
    private List<DetalleVentaDTO> detalle;

    @Schema(description = "Total (calculado por el backend)", example = "182.00")
    private Double total;
}
