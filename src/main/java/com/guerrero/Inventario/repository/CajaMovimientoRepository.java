package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.CajaMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CajaMovimientoRepository extends JpaRepository<CajaMovimiento, Long> {
    
    List<CajaMovimiento> findByCajaTurnoIdOrderByFechaAsc(Long cajaTurnoId);
}
