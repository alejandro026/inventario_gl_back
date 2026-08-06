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
@Table(name = "movimientos_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_kardex_producto"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_kardex_sucursal"))
    private Sucursal sucursal;

    @Column(nullable = false, length = 15)
    private String tipo; // ENTRADA, SALIDA

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, length = 50)
    private String motivo; // VENTA, COMPRA, AJUSTE_MANUAL, CANCELACION

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id",
            foreignKey = @ForeignKey(name = "fk_kardex_usuario"))
    private Usuario usuario;

    @Column(name = "referencia_id")
    private Long referenciaId; // e.g. ID de Venta
}
