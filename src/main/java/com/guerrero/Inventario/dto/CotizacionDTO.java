package com.guerrero.Inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para registrar y consultar cotizaciones")
public class CotizacionDTO {

    @Schema(description = "ID único de la cotización", example = "1")
    private Long id;

    @Schema(description = "Fecha de creación", example = "2026-08-28T18:16:03")
    private LocalDateTime fecha;

    @Schema(description = "Fecha de vencimiento (1 semana de validez)", example = "2026-09-04T18:16:03")
    private LocalDateTime fechaVencimiento;

    @NotNull(message = "La sucursal es obligatoria")
    @Schema(description = "ID de la sucursal de origen", example = "1")
    private Long idSucursal;

    @Schema(description = "Nombre de la sucursal", example = "Sucursal Principal")
    private String sucursalNombre;

    @Schema(description = "Nombre del cajero/usuario que generó la cotización", example = "Alejandro")
    private String usuarioNombre;

    @Schema(description = "ID del cliente (opcional)", example = "3")
    private Long idCliente;

    @Schema(description = "Nombre del cliente", example = "Público General")
    private String clienteNombre;

    @NotEmpty(message = "La cotización debe contener al menos un producto")
    @Valid
    private List<DetalleCotizacionDTO> detalle;

    @Schema(description = "Subtotal antes de descuento", example = "150.00")
    private BigDecimal subtotal;

    @Schema(description = "Descuento aplicado", example = "15.00")
    private BigDecimal descuento;

    @Schema(description = "Total final cotizado", example = "135.00")
    private BigDecimal total;

    @Schema(description = "Estado de la cotización (PENDIENTE, VENDIDA, VENCIDA)", example = "PENDIENTE")
    private String estado;

    @Schema(description = "ID de la venta si ya fue concretada", example = "200")
    private Long ventaId;
}
