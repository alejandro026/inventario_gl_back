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
@Table(name = "caja_movimientos")
public class CajaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caja_turno_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_movimiento_caja_turno"))
    private CajaTurno cajaTurno;

    @Column(nullable = false, length = 20)
    private String tipo; // INGRESO, EGRESO

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false, length = 255)
    private String concepto;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
