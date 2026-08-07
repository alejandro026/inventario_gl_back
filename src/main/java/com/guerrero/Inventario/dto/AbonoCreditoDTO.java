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
public class AbonoCreditoDTO {
    private Long id;
    private Long idCuenta;
    private BigDecimal monto;
    private LocalDateTime fecha;
    private String metodoPago;
}
