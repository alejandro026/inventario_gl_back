package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.SucursalDTO;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.mapper.SucursalMapper;
import com.guerrero.Inventario.model.Sucursal;
import com.guerrero.Inventario.repository.SucursalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SucursalService {

    private final SucursalRepository sucursalRepository;

    public SucursalService(SucursalRepository sucursalRepository) {
        this.sucursalRepository = sucursalRepository;
    }

    @Transactional(readOnly = true)
    public List<SucursalDTO> listar() {
        return sucursalRepository.findAll().stream()
                .map(SucursalMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SucursalDTO obtener(Long id) {
        return SucursalMapper.toDto(buscar(id));
    }

    public SucursalDTO crear(SucursalDTO dto) {
        if (sucursalRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new DuplicateResourceException("Ya existe una sucursal con nombre " + dto.getNombre());
        }
        Sucursal s = SucursalMapper.toEntity(dto);
        s.setId(null);
        return SucursalMapper.toDto(sucursalRepository.save(s));
    }

    public SucursalDTO actualizar(Long id, SucursalDTO dto) {
        Sucursal s = buscar(id);
        sucursalRepository.findByNombreIgnoreCase(dto.getNombre())
                .filter(o -> !o.getId().equals(id))
                .ifPresent(o -> {
                    throw new DuplicateResourceException("Ya existe otra sucursal con nombre " + dto.getNombre());
                });
        s.setNombre(dto.getNombre());
        s.setDireccion(dto.getDireccion());
        s.setTelefono(dto.getTelefono());
        return SucursalMapper.toDto(sucursalRepository.save(s));
    }

    public void eliminar(Long id) {
        Sucursal s = buscar(id);
        sucursalRepository.delete(s);
    }

    public Sucursal buscar(Long id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal", id));
    }
}
