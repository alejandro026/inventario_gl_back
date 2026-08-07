package com.guerrero.Inventario.mapper;

import com.guerrero.Inventario.dto.DetalleVentaDTO;
import com.guerrero.Inventario.dto.VentaDTO;
import com.guerrero.Inventario.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VentaMapperTest {

    private Producto producto() {
        Categoria categoria = new Categoria();
        categoria.setNombre("PAPELERIA");
        Producto p = new Producto();
        p.setId(1L);
        p.setNombre("Cuaderno");
        p.setPrecioCompra(new BigDecimal("20.00"));
        p.setCategoria(categoria);
        return p;
    }

    @Test
    void toDtoDetalle_null_devuelveNull() {
        assertThat(VentaMapper.toDto((DetalleVenta) null)).isNull();
    }

    @Test
    void toDtoDetalle_mapeaCampos() {
        DetalleVenta d = new DetalleVenta();
        d.setId(5L);
        d.setProducto(producto());
        d.setCantProd(2);
        d.setPrecio(new BigDecimal("45.50"));
        d.setPrecioCompra(new BigDecimal("25.00"));
        d.setSubtotal(new BigDecimal("91.00"));

        DetalleVentaDTO dto = VentaMapper.toDto(d);

        assertThat(dto.getProductoId()).isEqualTo(1L);
        assertThat(dto.getNombreProd()).isEqualTo("Cuaderno");
        assertThat(dto.getCategoriaProd()).isEqualTo("PAPELERIA");
        assertThat(dto.getPrecioCompra()).isEqualByComparingTo("25.00");
    }

    @Test
    void toDtoDetalle_sinPrecioCompraPropio_usaElDelProducto() {
        DetalleVenta d = new DetalleVenta();
        d.setProducto(producto());
        d.setCantProd(1);
        d.setPrecio(new BigDecimal("50.00"));
        d.setPrecioCompra(null);
        d.setSubtotal(new BigDecimal("50.00"));

        DetalleVentaDTO dto = VentaMapper.toDto(d);

        assertThat(dto.getPrecioCompra()).isEqualByComparingTo("20.00");
    }

    @Test
    void toDtoDetalle_sinProductoNiPrecioCompra_defaultCero() {
        DetalleVenta d = new DetalleVenta();
        d.setProducto(null);
        d.setCantProd(1);
        d.setPrecio(new BigDecimal("50.00"));
        d.setPrecioCompra(null);
        d.setSubtotal(new BigDecimal("50.00"));

        DetalleVentaDTO dto = VentaMapper.toDto(d);

        assertThat(dto.getPrecioCompra()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getProductoId()).isNull();
    }

    @Test
    void toDtoVenta_null_devuelveNull() {
        assertThat(VentaMapper.toDto((Venta) null)).isNull();
    }

    @Test
    void toDtoVenta_mapeaCamposYDetalles() {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(3L);
        Usuario usuario = new Usuario();
        usuario.setNombre("Empleado Uno");
        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setNombre("Juan Perez");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto());
        detalle.setCantProd(1);
        detalle.setPrecio(BigDecimal.TEN);
        detalle.setPrecioCompra(BigDecimal.ONE);
        detalle.setSubtotal(BigDecimal.TEN);

        Venta venta = new Venta();
        venta.setId(100L);
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(Venta.EstadoVenta.COMPLETADA);
        venta.setMetodoPago(Venta.MetodoPago.CREDITO);
        venta.setTotal(new BigDecimal("10.00"));
        venta.setPagoCon(BigDecimal.ZERO);
        venta.setCambio(BigDecimal.ZERO);
        venta.setSucursal(sucursal);
        venta.setUsuario(usuario);
        venta.setCliente(cliente);
        venta.setDetalle(new ArrayList<>(List.of(detalle)));

        VentaDTO dto = VentaMapper.toDto(venta);

        assertThat(dto.getEstado()).isEqualTo("COMPLETADA");
        assertThat(dto.getMetodoPago()).isEqualTo("CREDITO");
        assertThat(dto.getIdSucursal()).isEqualTo(3L);
        assertThat(dto.getUsuarioNombre()).isEqualTo("Empleado Uno");
        assertThat(dto.getIdCliente()).isEqualTo(7L);
        assertThat(dto.getClienteNombre()).isEqualTo("Juan Perez");
        assertThat(dto.getDetalle()).hasSize(1);
    }

    @Test
    void toDtoVenta_sinClienteNiUsuario_noLanzaExcepcion() {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(3L);

        Venta venta = new Venta();
        venta.setId(101L);
        venta.setEstado(Venta.EstadoVenta.PENDIENTE);
        venta.setSucursal(sucursal);
        venta.setDetalle(new ArrayList<>());

        VentaDTO dto = VentaMapper.toDto(venta);

        assertThat(dto.getUsuarioNombre()).isNull();
        assertThat(dto.getIdCliente()).isNull();
        assertThat(dto.getDetalle()).isEmpty();
    }
}
