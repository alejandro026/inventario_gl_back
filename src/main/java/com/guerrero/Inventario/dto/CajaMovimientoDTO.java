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
public class CajaMovimientoDTO {
    private Long id;
    private Long idCajaTurno;
    private String tipo;
    private BigDecimal monto;
    private String concepto;
    private LocalDateTime fecha;
}
