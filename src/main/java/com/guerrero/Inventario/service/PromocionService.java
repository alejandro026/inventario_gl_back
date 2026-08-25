package com.guerrero.Inventario.service;

import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.Promocion;
import com.guerrero.Inventario.repository.PromocionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PromocionService {

    private static final Logger log = LoggerFactory.getLogger(PromocionService.class);

    private final PromocionRepository repository;

    public PromocionService(PromocionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Promocion> listarTodas() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Promocion obtenerActiva(LocalDateTime fecha) {
        List<Promocion> active = repository.findActivePromotions(fecha != null ? fecha : LocalDateTime.now());
        if (active.isEmpty()) {
            return null;
        }
        return active.get(0);
    }

    @Transactional(readOnly = true)
    public Promocion obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción", id));
    }

    public Promocion guardar(Promocion promo) {
        validarPromocion(promo);

        // Validar solapamiento si la promoción está activa
        if (Boolean.TRUE.equals(promo.getActiva())) {
            List<Promocion> overlapping = repository.findOverlappingPromotions(promo.getFechaInicio(), promo.getFechaFin());
            boolean hasOverlap = overlapping.stream()
                    .anyMatch(p -> !p.getId().equals(promo.getId()));
            if (hasOverlap) {
                log.warn("Intento de guardar promoción solapada: '{}'. Fechas: {} - {}", 
                        promo.getNombre(), promo.getFechaInicio(), promo.getFechaFin());
                throw new BusinessException("Ya existe una promoción activa configurada dentro de este rango de fechas.");
            }
        }

        log.info("Guardando promoción: '{}'. Descuento: {}%, Compra mínima: {}", 
                promo.getNombre(), promo.getPorcentajeDescuento(), promo.getCompraMinima());
        return repository.save(promo);
    }

    public void eliminar(Long id) {
        Promocion p = obtenerPorId(id);
        log.info("Eliminando promoción con ID: {} ('{}')", id, p.getNombre());
        repository.delete(p);
    }

    private void validarPromocion(Promocion p) {
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new BusinessException("El nombre de la promoción es obligatorio.");
        }
        if (p.getFechaInicio() == null || p.getFechaFin() == null) {
            throw new BusinessException("Las fechas de inicio y fin son obligatorias.");
        }
        if (p.getFechaFin().isBefore(p.getFechaInicio())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
        if (p.getCompraMinima() == null || p.getCompraMinima().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El monto de compra mínima no puede ser negativo.");
        }
        if (p.getPorcentajeDescuento() == null || 
            p.getPorcentajeDescuento().compareTo(BigDecimal.ZERO) < 0 || 
            p.getPorcentajeDescuento().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("El porcentaje de descuento debe estar entre 0% y 100%.");
        }
    }
}
