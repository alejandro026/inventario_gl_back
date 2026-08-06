package com.guerrero.Inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MovimientoInventarioDTO {
    private Long id;
    private Long idProducto;
    private String productoNombre;
    private String productoCodigo;
    private Long idSucursal;
    private String tipo;
    private Integer cantidad;
    private String motivo;
    private LocalDateTime fecha;
    private String usuarioNombre;
    private Long referenciaId;
}
