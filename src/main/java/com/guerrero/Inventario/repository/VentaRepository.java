package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Page<Venta> findBySucursal_Id(Long sucursalId, Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin")
    List<Venta> findByFechaBetween(@Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v " +
            "WHERE v.estado = com.guerrero.Inventario.model.Venta.EstadoVenta.COMPLETADA " +
            "AND v.fecha BETWEEN :inicio AND :fin")
    Double totalVendidoEntre(@Param("inicio") LocalDateTime inicio,
                             @Param("fin") LocalDateTime fin);
}
