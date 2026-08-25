package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    @Query("SELECT p FROM Promocion p WHERE p.activa = true AND :fecha BETWEEN p.fechaInicio AND p.fechaFin ORDER BY p.id DESC")
    List<Promocion> findActivePromotions(@Param("fecha") LocalDateTime fecha);

    @Query("SELECT p FROM Promocion p WHERE p.activa = true AND " +
           "((p.fechaInicio <= :fin AND p.fechaFin >= :inicio))")
    List<Promocion> findOverlappingPromotions(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
