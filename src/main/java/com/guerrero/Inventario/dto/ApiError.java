package com.guerrero.Inventario.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta estandar de error")
public class ApiError {

    @Schema(example = "2026-05-20T10:30:00")
    private LocalDateTime timestamp;

    @Schema(example = "404")
    private Integer status;

    @Schema(example = "Not Found")
    private String error;

    @Schema(example = "Producto no encontrado con id 99")
    private String message;

    @Schema(example = "/api/productos/99")
    private String path;

    @Schema(description = "Errores de validacion campo->mensaje")
    private Map<String, String> validationErrors;

    @Schema(description = "Detalles adicionales")
    private List<String> details;
}
