package com.guerrero.Inventario.controller;

import com.guerrero.Inventario.dto.ReporteFinanceDTO;
import com.guerrero.Inventario.service.ReporteFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reportes/finanzas")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Finanzas", description = "Reportes financieros y ganancias del negocio (ADMIN)")
public class ReporteFinanceController {

    private final ReporteFinanceService financeService;

    public ReporteFinanceController(ReporteFinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    @Operation(summary = "Obtener reporte de ingresos, costos, utilidades y margen del negocio")
    public ReporteFinanceDTO obtenerReporte(
            @Parameter(description = "Fecha de inicio del rango (por defecto inicio del mes actual)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            
            @Parameter(description = "Fecha de fin del rango (por defecto fecha/hora actual)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {

        LocalDateTime start = inicio != null ? inicio : LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = fin != null ? fin : LocalDateTime.now();

        return financeService.obtenerMétricasFinancieras(start, end);
    }
}
