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
@Table(name = "abonos_credito")
public class AbonoCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_abono_cxc"))
    private CuentaPorCobrar cuentaPorCobrar;

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago; // EFECTIVO, TARJETA, TRANSFERENCIA
}
