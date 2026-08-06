package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.AbonoCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AbonoCreditoRepository extends JpaRepository<AbonoCredito, Long> {
    
    List<AbonoCredito> findByCuentaPorCobrarIdOrderByFechaDesc(Long cuentaId);
}
