package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.MovimientoInventarioDTO;
import com.guerrero.Inventario.model.MovimientoInventario;
import com.guerrero.Inventario.model.Producto;
import com.guerrero.Inventario.model.Sucursal;
import com.guerrero.Inventario.model.Usuario;
import com.guerrero.Inventario.repository.MovimientoInventarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KardexServiceTest {

    @Mock
    private MovimientoInventarioRepository repository;

    @InjectMocks
    private KardexService service;

    private Producto producto() {
        Producto p = new Producto();
        p.setId(1L);
        p.setNombre("Cuaderno");
        p.setCodigo("C1");
        return p;
    }

    private Sucursal sucursal() {
        Sucursal s = new Sucursal();
        s.setId(2L);
        return s;
    }

    @Test
    void registrarMovimiento_normalizaTipoYMotivoAMayusculas() {
        when(repository.save(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarMovimiento(producto(), sucursal(), "entrada", 5, "ajuste_manual", null, null);

        verify(repository).save(argThat(m -> m.getTipo().equals("ENTRADA") && m.getMotivo().equals("AJUSTE_MANUAL")));
    }

    @Test
    void registrarMovimiento_conUsuarioYReferencia_persisteAmbos() {
        Usuario u = new Usuario();
        u.setId(9L);
        when(repository.save(any(MovimientoInventario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarMovimiento(producto(), sucursal(), "SALIDA", 3, "VENTA", u, 100L);

        verify(repository).save(argThat(m -> m.getUsuario() == u && m.getReferenciaId().equals(100L)));
    }

    @Test
    void listarPorProductoAndSucursal_mapeaDto() {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setId(1L);
        mov.setProducto(producto());
        mov.setSucursal(sucursal());
        mov.setTipo("ENTRADA");
        mov.setCantidad(5);
        mov.setMotivo("AJUSTE_MANUAL");

        when(repository.findByProductoIdAndSucursalIdOrderByFechaDesc(1L, 2L)).thenReturn(List.of(mov));

        List<MovimientoInventarioDTO> resultado = service.listarPorProductoAndSucursal(1L, 2L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getProductoNombre()).isEqualTo("Cuaderno");
        assertThat(resultado.get(0).getUsuarioNombre()).isNull();
    }

    @Test
    void listarPorProducto_mapeaDto() {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setId(2L);
        mov.setProducto(producto());
        mov.setSucursal(sucursal());
        mov.setTipo("SALIDA");
        mov.setCantidad(1);
        mov.setMotivo("VENTA");

        when(repository.findByProductoIdOrderByFechaDesc(1L)).thenReturn(List.of(mov));

        List<MovimientoInventarioDTO> resultado = service.listarPorProducto(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTipo()).isEqualTo("SALIDA");
    }
}
