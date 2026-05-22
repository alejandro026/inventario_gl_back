package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.CategoriaDTO;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.mapper.CategoriaMapper;
import com.guerrero.Inventario.model.Categoria;
import com.guerrero.Inventario.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listar() {
        return categoriaRepository.findAll().stream()
                .map(CategoriaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoriaDTO obtener(Long id) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", id));
        return CategoriaMapper.toDto(c);
    }

    public CategoriaDTO crear(CategoriaDTO dto) {
        String nombre = dto.getNombre().trim().toUpperCase();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Ya existe una categoria con nombre " + nombre);
        }
        Categoria entity = CategoriaMapper.toEntity(dto);
        entity.setNombre(nombre);
        return CategoriaMapper.toDto(categoriaRepository.save(entity));
    }

    public CategoriaDTO actualizar(Long id, CategoriaDTO dto) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", id));
        String nuevoNombre = dto.getNombre().trim().toUpperCase();
        categoriaRepository.findByNombreIgnoreCase(nuevoNombre)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Ya existe otra categoria con nombre " + nuevoNombre);
                });
        c.setNombre(nuevoNombre);
        c.setDescripcion(dto.getDescripcion());
        return CategoriaMapper.toDto(categoriaRepository.save(c));
    }

    public void eliminar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Categoria", id);
        }
        categoriaRepository.deleteById(id);
    }
}
