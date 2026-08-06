package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.CuentaPorCobrar;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {
    
    List<CuentaPorCobrar> findByClienteIdAndEstado(Long clienteId, String estado);
    
    List<CuentaPorCobrar> findByClienteId(Long clienteId);
}
