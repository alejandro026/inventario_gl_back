package com.guerrero.Inventario.repository;

import com.guerrero.Inventario.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}
