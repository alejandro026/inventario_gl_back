package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    Page<Producto> findByCategoria_Id(Long categoriaId, Pageable pageable);

    Page<Producto> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    @Query("SELECT p FROM Producto p WHERE p.stockMinimo IS NOT NULL AND p.cantidad <= p.stockMinimo")
    List<Producto> findProductosBajoStock();

    @Query("SELECT p FROM Producto p WHERE p.categoria.nombre = :nombreCategoria")
    List<Producto> findByCategoriaNombre(@Param("nombreCategoria") String nombreCategoria);
}
