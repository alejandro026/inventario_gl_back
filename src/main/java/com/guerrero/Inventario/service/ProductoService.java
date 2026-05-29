package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.ProductoDTO;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.DuplicateResourceException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.mapper.ProductoMapper;
import com.guerrero.Inventario.model.Categoria;
import com.guerrero.Inventario.model.Producto;
import com.guerrero.Inventario.repository.CategoriaRepository;
import com.guerrero.Inventario.repository.IProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoService {

    private final IProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(IProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductoDTO> listar(Pageable pageable, Long categoriaId, String buscar) {
        Page<Producto> page;
        if (categoriaId != null) {
            page = productoRepository.findByCategoria_Id(categoriaId, pageable);
        } else if (buscar != null && !buscar.isBlank()) {
            page = productoRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(buscar, buscar, pageable);
        } else {
            page = productoRepository.findAll(pageable);
        }
        return page.map(ProductoMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProductoDTO obtener(Long id) {
        return ProductoMapper.toDto(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<ProductoDTO> productosBajoStock() {
        return productoRepository.findProductosBajoStock().stream()
                .map(ProductoMapper::toDto)
                .collect(Collectors.toList());
    }

    public ProductoDTO crear(ProductoDTO dto) {
        if (productoRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new DuplicateResourceException("Ya existe un producto con nombre " + dto.getNombre());
        }
        if (dto.getCodigo() != null && !dto.getCodigo().isBlank() 
                && productoRepository.existsByCodigoIgnoreCase(dto.getCodigo())) {
            throw new DuplicateResourceException("Ya existe un producto con el código " + dto.getCodigo());
        }
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", dto.getCategoriaId()));
        Producto entity = ProductoMapper.toEntity(dto, categoria);
        entity.setId(null);
        return ProductoMapper.toDto(productoRepository.save(entity));
    }

    public ProductoDTO actualizar(Long id, ProductoDTO dto) {
        Producto p = buscar(id);
        if (!p.getNombre().equalsIgnoreCase(dto.getNombre())
                && productoRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new DuplicateResourceException("Ya existe otro producto con nombre " + dto.getNombre());
        }
        if (dto.getCodigo() != null && !dto.getCodigo().isBlank()) {
            if ((p.getCodigo() == null || !p.getCodigo().equalsIgnoreCase(dto.getCodigo()))
                    && productoRepository.existsByCodigoIgnoreCase(dto.getCodigo())) {
                throw new DuplicateResourceException("Ya existe otro producto con el código " + dto.getCodigo());
            }
            p.setCodigo(dto.getCodigo());
        } else {
            p.setCodigo(null);
        }
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", dto.getCategoriaId()));
        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        p.setPrecio(dto.getPrecio());
        p.setPrecioCompra(dto.getPrecioCompra() == null ? 0.0 : dto.getPrecioCompra());
        p.setCantidad(dto.getCantidad());
        p.setStockMinimo(dto.getStockMinimo());
        p.setActivo(dto.getActivo() == null ? p.getActivo() : dto.getActivo());
        p.setControlaStock(dto.getControlaStock() == null ? Boolean.TRUE : dto.getControlaStock());
        p.setCategoria(categoria);
        return ProductoMapper.toDto(productoRepository.save(p));
    }

    public ProductoDTO ajustarStock(Long id, Integer delta) {
        if (delta == null || delta == 0) {
            throw new BusinessException("El ajuste de stock no puede ser cero");
        }
        Producto p = buscar(id);
        int nuevo = p.getCantidad() + delta;
        if (nuevo < 0) {
            throw new BusinessException("El stock resultante no puede ser negativo");
        }
        p.setCantidad(nuevo);
        return ProductoMapper.toDto(productoRepository.save(p));
    }

    public void eliminar(Long id) {
        Producto p = buscar(id);
        productoRepository.delete(p);
    }

    /** Acceso interno: devuelve la entidad o lanza 404. */
    public Producto buscar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }
}
