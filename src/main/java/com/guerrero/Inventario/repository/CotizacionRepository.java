package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    List<Cotizacion> findBySucursalIdOrderByFechaDesc(Long sucursalId);

    @Modifying
    @Query("UPDATE Cotizacion c SET c.estado = 'VENCIDA' " +
           "WHERE c.estado = 'PENDIENTE' AND c.fechaVencimiento < :ahora")
    void expirarCotizacionesVencidas(@Param("ahora") LocalDateTime ahora);
}
