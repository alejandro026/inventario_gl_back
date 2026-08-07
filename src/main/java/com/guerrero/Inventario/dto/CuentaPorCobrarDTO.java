package com.guerrero.Inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CuentaPorCobrarDTO {
    private Long id;
    private Long idCliente;
    private String clienteNombre;
    private Long idVenta;
    private LocalDateTime ventaFecha;
    private BigDecimal montoTotal;
    private BigDecimal saldoPendiente;
    private String estado;
}
