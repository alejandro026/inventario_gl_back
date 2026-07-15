package com.guerrero.Inventario.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cuentas_por_cobrar")
public class CuentaPorCobrar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cxc_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cxc_venta"))
    private Venta venta;

    @Column(name = "monto_total", nullable = false)
    private Double montoTotal;

    @Column(name = "saldo_pendiente", nullable = false)
    private Double saldoPendiente;

    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE"; // PENDIENTE, PAGADO
}
