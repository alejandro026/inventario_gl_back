package com.guerrero.Inventario.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Column(name = "monto_apertura", nullable = false)
    private Double montoApertura = 0.0;

    @Column(name = "monto_cierre_teorico", nullable = false)
    private Double montoCierreTeorico = 0.0;

    @Column(name = "monto_cierre_real", nullable = false)
    private Double montoCierreReal = 0.0;

    @Column(name = "diferencia", nullable = false)
    private Double diferencia = 0.0;

    @Column(nullable = false, length = 20)
    private String estado = "ABIERTO"; // ABIERTO, CERRADO

    @Column(length = 500)
    private String notas;
}
