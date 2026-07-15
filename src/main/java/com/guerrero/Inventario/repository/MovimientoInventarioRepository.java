package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    
    List<MovimientoInventario> findByProductoIdAndSucursalIdOrderByFechaDesc(Long productoId, Long sucursalId);
    
    List<MovimientoInventario> findByProductoIdOrderByFechaDesc(Long productoId);
}
