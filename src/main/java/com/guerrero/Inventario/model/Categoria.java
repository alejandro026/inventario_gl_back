package com.guerrero.Inventario.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categorias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categoria_nombre", columnNames = "nombre")
})
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL")
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion",
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL")
    private LocalDateTime fechaActualizacion;
}
