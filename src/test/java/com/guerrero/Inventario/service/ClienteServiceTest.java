package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.AbonoCreditoDTO;
import com.guerrero.Inventario.dto.ClienteDTO;
import com.guerrero.Inventario.dto.CuentaPorCobrarDTO;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.AbonoCredito;
import com.guerrero.Inventario.model.Cliente;
import com.guerrero.Inventario.model.CuentaPorCobrar;
import com.guerrero.Inventario.model.Venta;
import com.guerrero.Inventario.repository.AbonoCreditoRepository;
import com.guerrero.Inventario.repository.ClienteRepository;
import com.guerrero.Inventario.repository.CuentaPorCobrarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaPorCobrarRepository cuentaPorCobrarRepository;
    @Mock
    private AbonoCreditoRepository abonoCreditoRepository;

    @InjectMocks
    private ClienteService service;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Juan Perez");
        cliente.setLimiteCredito(new BigDecimal("500.00"));
        cliente.setSaldoPendiente(new BigDecimal("100.00"));
    }

    @Test
    void listarTodos_mapeaLista() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        List<ClienteDTO> resultado = service.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Juan Perez");
    }

    @Test
    void buscarPorNombre_delegaAlRepositorio() {
        when(clienteRepository.findByNombreContainingIgnoreCase("juan")).thenReturn(List.of(cliente));

        List<ClienteDTO> resultado = service.buscarPorNombre("juan");

        assertThat(resultado).hasSize(1);
    }

    @Test
    void buscar_noExistente_lanzaResourceNotFound() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_sinLimiteCredito_defaultCero() {
        ClienteDTO dto = new ClienteDTO();
        dto.setNombre("Nuevo Cliente");
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteDTO creado = service.crear(dto);

        assertThat(creado.getLimiteCredito()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(creado.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void actualizar_existente_guardaCambios() {
        ClienteDTO dto = new ClienteDTO();
        dto.setNombre("Juan Actualizado");
        dto.setLimiteCredito(new BigDecimal("1000.00"));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteDTO actualizado = service.actualizar(1L, dto);

        assertThat(actualizado.getNombre()).isEqualTo("Juan Actualizado");
        assertThat(actualizado.getLimiteCredito()).isEqualByComparingTo("1000.00");
    }

    @Test
    void eliminar_existente_llamaDelete() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        service.eliminar(1L);

        verify(clienteRepository).delete(cliente);
    }

    @Test
    void obtenerCuentasPorCobrar_mapeaLista() {
        Venta venta = new Venta();
        venta.setId(5L);
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(2L);
        cxc.setCliente(cliente);
        cxc.setVenta(venta);
        cxc.setMontoTotal(new BigDecimal("200.00"));
        cxc.setSaldoPendiente(new BigDecimal("200.00"));
        cxc.setEstado("PENDIENTE");

        when(cuentaPorCobrarRepository.findByClienteId(1L)).thenReturn(List.of(cxc));

        List<CuentaPorCobrarDTO> resultado = service.obtenerCuentasPorCobrar(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMontoTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    void registrarAbono_parcial_reduceSaldos() {
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(2L);
        cxc.setCliente(cliente);
        cxc.setSaldoPendiente(new BigDecimal("100.00"));
        cxc.setEstado("PENDIENTE");

        when(cuentaPorCobrarRepository.findById(2L)).thenReturn(Optional.of(cxc));
        when(abonoCreditoRepository.save(any(AbonoCredito.class))).thenAnswer(inv -> {
            AbonoCredito a = inv.getArgument(0);
            a.setId(9L);
            return a;
        });

        AbonoCreditoDTO dto = service.registrarAbono(2L, new BigDecimal("40.00"), "EFECTIVO");

        assertThat(dto.getMonto()).isEqualByComparingTo("40.00");
        assertThat(cxc.getSaldoPendiente()).isEqualByComparingTo("60.00");
        assertThat(cxc.getEstado()).isEqualTo("PENDIENTE");
        assertThat(cliente.getSaldoPendiente()).isEqualByComparingTo("60.00");
    }

    @Test
    void registrarAbono_pagaSaldoCompleto_marcaComoPagado() {
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(2L);
        cxc.setCliente(cliente);
        cxc.setSaldoPendiente(new BigDecimal("100.00"));
        cxc.setEstado("PENDIENTE");

        when(cuentaPorCobrarRepository.findById(2L)).thenReturn(Optional.of(cxc));
        when(abonoCreditoRepository.save(any(AbonoCredito.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarAbono(2L, new BigDecimal("100.00"), "EFECTIVO");

        assertThat(cxc.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cxc.getEstado()).isEqualTo("PAGADO");
    }

    @Test
    void registrarAbono_montoMayorAlSaldo_noQuedaNegativo() {
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(2L);
        cxc.setCliente(cliente);
        cxc.setSaldoPendiente(new BigDecimal("30.00"));
        cxc.setEstado("PENDIENTE");

        when(cuentaPorCobrarRepository.findById(2L)).thenReturn(Optional.of(cxc));
        when(abonoCreditoRepository.save(any(AbonoCredito.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarAbono(2L, new BigDecimal("500.00"), "EFECTIVO");

        assertThat(cxc.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cliente.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registrarAbono_cuentaYaPagada_lanzaIllegalStateException() {
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(2L);
        cxc.setEstado("PAGADO");

        when(cuentaPorCobrarRepository.findById(2L)).thenReturn(Optional.of(cxc));

        assertThatThrownBy(() -> service.registrarAbono(2L, new BigDecimal("10.00"), "EFECTIVO"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void obtenerHistorialAbonos_mapeaLista() {
        AbonoCredito abono = new AbonoCredito();
        abono.setId(1L);
        CuentaPorCobrar cxc = new CuentaPorCobrar();
        cxc.setId(2L);
        abono.setCuentaPorCobrar(cxc);
        abono.setMonto(new BigDecimal("40.00"));
        abono.setMetodoPago("EFECTIVO");

        when(abonoCreditoRepository.findByCuentaPorCobrarIdOrderByFechaDesc(2L)).thenReturn(List.of(abono));

        List<AbonoCreditoDTO> resultado = service.obtenerHistorialAbonos(2L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMonto()).isEqualByComparingTo("40.00");
    }
}
