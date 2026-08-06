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
public class CajaTurnoDTO {
    private Long id;
    private Long idSucursal;
    private String sucursalNombre;
    private Long idUsuario;
    private String usuarioNombre;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private Double montoApertura;
    private Double montoCierreTeorico;
    private Double montoCierreReal;
    private Double diferencia;
    private String estado;
    private String notas;
    private java.util.Map<String, Double> desgloseMetodosPago;
    private java.util.Map<String, Double> desgloseCategorias;
}
