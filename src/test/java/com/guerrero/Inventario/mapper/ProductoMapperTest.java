package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.ProductoDTO;
import com.guerrero.Inventario.model.Categoria;
import com.guerrero.Inventario.model.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoMapperTest {

    @Test
    void toDto_null_devuelveNull() {
        assertThat(ProductoMapper.toDto(null)).isNull();
    }

    @Test
    void toDto_mapeaTodosLosCampos() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("PAPELERIA");

        Producto p = new Producto();
        p.setId(10L);
        p.setCodigo("C1");
        p.setNombre("Cuaderno");
        p.setDescripcion("Descripcion");
        p.setPrecio(new BigDecimal("45.50"));
        p.setPrecioCompra(new BigDecimal("25.00"));
        p.setCantidad(100);
        p.setStockMinimo(5);
        p.setActivo(true);
        p.setControlaStock(true);
        p.setCategoria(categoria);

        ProductoDTO dto = ProductoMapper.toDto(p);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getCodigo()).isEqualTo("C1");
        assertThat(dto.getNombre()).isEqualTo("Cuaderno");
        assertThat(dto.getPrecio()).isEqualByComparingTo("45.50");
        assertThat(dto.getCategoriaId()).isEqualTo(1L);
        assertThat(dto.getCategoriaNombre()).isEqualTo("PAPELERIA");
    }

    @Test
    void toDto_controlaStockNull_defaultTrue() {
        Producto p = new Producto();
        p.setControlaStock(null);

        ProductoDTO dto = ProductoMapper.toDto(p);

        assertThat(dto.getControlaStock()).isTrue();
    }

    @Test
    void toEntity_null_devuelveNull() {
        assertThat(ProductoMapper.toEntity(null, null)).isNull();
    }

    @Test
    void toEntity_mapeaCamposYAplicaDefaults() {
        Categoria categoria = new Categoria();
        categoria.setId(2L);

        ProductoDTO dto = new ProductoDTO();
        dto.setNombre("Lapiz");
        dto.setPrecio(new BigDecimal("5.00"));
        dto.setPrecioCompra(null);
        dto.setCantidad(50);
        dto.setActivo(null);
        dto.setControlaStock(null);

        Producto entity = ProductoMapper.toEntity(dto, categoria);

        assertThat(entity.getNombre()).isEqualTo("Lapiz");
        assertThat(entity.getPrecioCompra()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(entity.getActivo()).isTrue();
        assertThat(entity.getControlaStock()).isTrue();
        assertThat(entity.getCategoria()).isEqualTo(categoria);
    }
}
