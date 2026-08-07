package com.guerrero.Inventario.model;

import jakarta.persistence.*;
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
@Entity
@Table(name = "caja_turnos")
public class CajaTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_caja_turno_sucursal"))
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_caja_turno_usuario"))
    private Usuario usuario;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_apertura", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoApertura = BigDecimal.ZERO;

    @Column(name = "monto_cierre_teorico", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoCierreTeorico = BigDecimal.ZERO;

    @Column(name = "monto_cierre_real", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoCierreReal = BigDecimal.ZERO;

    @Column(name = "diferencia", nullable = false, precision = 12, scale = 2)
    private BigDecimal diferencia = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String estado = "ABIERTO"; // ABIERTO, CERRADO

    @Column(length = 500)
    private String notas;
}
