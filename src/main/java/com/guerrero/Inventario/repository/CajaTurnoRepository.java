package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.CajaTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CajaTurnoRepository extends JpaRepository<CajaTurno, Long> {
    
    Optional<CajaTurno> findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(Long sucursalId, Long usuarioId, String estado);
    
    List<CajaTurno> findBySucursalIdOrderByFechaAperturaDesc(Long sucursalId);
}
