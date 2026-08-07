package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.CajaMovimientoDTO;
import com.guerrero.Inventario.dto.CajaTurnoDTO;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.*;
import com.guerrero.Inventario.repository.*;
import com.guerrero.Inventario.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    @Mock
    private CajaTurnoRepository cajaTurnoRepository;
    @Mock
    private CajaMovimientoRepository cajaMovimientoRepository;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CajaService service;

    private Sucursal sucursal;
    private Usuario empleado;
    private Usuario admin;
    private CajaTurno turnoAbierto;

    @BeforeEach
    void setUp() {
        sucursal = new Sucursal();
        sucursal.setId(1L);
        sucursal.setNombre("Centro");

        empleado = new Usuario();
        empleado.setId(9L);
        empleado.setNombre("Empleado Uno");
        empleado.setRol(Rol.EMPLEADO);
        empleado.setSucursal(sucursal);

        admin = new Usuario();
        admin.setId(1L);
        admin.setNombre("Admin");
        admin.setRol(Rol.ADMIN);

        turnoAbierto = new CajaTurno();
        turnoAbierto.setId(50L);
        turnoAbierto.setSucursal(sucursal);
        turnoAbierto.setUsuario(empleado);
        turnoAbierto.setFechaApertura(LocalDateTime.now().minusHours(1));
        turnoAbierto.setMontoApertura(new BigDecimal("100.00"));
        turnoAbierto.setMontoCierreTeorico(new BigDecimal("100.00"));
        turnoAbierto.setMontoCierreReal(BigDecimal.ZERO);
        turnoAbierto.setDiferencia(BigDecimal.ZERO);
        turnoAbierto.setEstado("ABIERTO");
    }

    private void stubSinVentasNiMovimientos() {
        when(ventaRepository.findSalesInTurn(anyLong(), anyLong(), any(), any())).thenReturn(List.of());
        when(cajaMovimientoRepository.findByCajaTurnoIdOrderByFechaAsc(anyLong())).thenReturn(List.of());
    }

    @Test
    void obtenerEstadoActual_empleado_usaSuPropioIdIgnorandoElSolicitado() {
        when(currentUserProvider.obtenerOFallar()).thenReturn(empleado);
        when(cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(1L, 9L, "ABIERTO"))
                .thenReturn(Optional.of(turnoAbierto));
        stubSinVentasNiMovimientos();

        CajaTurnoDTO dto = service.obtenerEstadoActual(1L, 999L);

        assertThat(dto.getIdUsuario()).isEqualTo(9L);
        verify(cajaTurnoRepository).findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(1L, 9L, "ABIERTO");
    }

    @Test
    void obtenerEstadoActual_admin_puedeConsultarOtroUsuario() {
        when(currentUserProvider.obtenerOFallar()).thenReturn(admin);
        when(cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(1L, 9L, "ABIERTO"))
                .thenReturn(Optional.of(turnoAbierto));
        stubSinVentasNiMovimientos();

        CajaTurnoDTO dto = service.obtenerEstadoActual(1L, 9L);

        assertThat(dto).isNotNull();
    }

    @Test
    void obtenerEstadoActual_sinTurnoAbierto_devuelveNull() {
        when(currentUserProvider.obtenerOFallar()).thenReturn(empleado);
        when(cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(1L, 9L, "ABIERTO"))
                .thenReturn(Optional.empty());

        CajaTurnoDTO dto = service.obtenerEstadoActual(1L, null);

        assertThat(dto).isNull();
    }

    @Test
    void apertura_exitosa_creaTurnoAbierto() {
        when(currentUserProvider.obtenerOFallar()).thenReturn(empleado);
        when(cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(1L, 9L, "ABIERTO"))
                .thenReturn(Optional.empty());
        when(sucursalRepository.findById(1L)).thenReturn(Optional.of(sucursal));
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(empleado));
        when(cajaTurnoRepository.save(any(CajaTurno.class))).thenAnswer(inv -> {
            CajaTurno t = inv.getArgument(0);
            t.setId(60L);
            return t;
        });
        stubSinVentasNiMovimientos();

        CajaTurnoDTO dto = service.apertura(1L, null, new BigDecimal("200.00"));

        assertThat(dto.getEstado()).isEqualTo("ABIERTO");
        assertThat(dto.getMontoApertura()).isEqualByComparingTo("200.00");
    }

    @Test
    void apertura_yaExisteTurnoAbierto_lanzaIllegalStateException() {
        when(currentUserProvider.obtenerOFallar()).thenReturn(empleado);
        when(cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(1L, 9L, "ABIERTO"))
                .thenReturn(Optional.of(turnoAbierto));

        assertThatThrownBy(() -> service.apertura(1L, null, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
        verify(cajaTurnoRepository, never()).save(any());
    }

    @Test
    void registrarMovimiento_montoNuloOCero_lanzaBusinessException() {
        assertThatThrownBy(() -> service.registrarMovimiento(50L, "INGRESO", null, "x"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.registrarMovimiento(50L, "INGRESO", BigDecimal.ZERO, "x"))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(cajaTurnoRepository);
    }

    @Test
    void registrarMovimiento_tipoInvalido_lanzaBusinessException() {
        assertThatThrownBy(() -> service.registrarMovimiento(50L, "TRANSFERENCIA", BigDecimal.TEN, "x"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registrarMovimiento_turnoInexistente_lanzaResourceNotFound() {
        when(cajaTurnoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarMovimiento(999L, "INGRESO", BigDecimal.TEN, "x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void registrarMovimiento_turnoDeOtroUsuarioSinSerAdmin_lanzaAccessDenied() {
        Usuario otro = new Usuario();
        otro.setId(77L);
        otro.setRol(Rol.EMPLEADO);

        when(cajaTurnoRepository.findById(50L)).thenReturn(Optional.of(turnoAbierto));
        when(currentUserProvider.obtenerOFallar()).thenReturn(otro);

        assertThatThrownBy(() -> service.registrarMovimiento(50L, "INGRESO", BigDecimal.TEN, "x"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void registrarMovimiento_turnoCerrado_lanzaIllegalStateException() {
        turnoAbierto.setEstado("CERRADO");
        when(cajaTurnoRepository.findById(50L)).thenReturn(Optional.of(turnoAbierto));
        when(currentUserProvider.obtenerOFallar()).thenReturn(empleado);

        assertThatThrownBy(() -> service.registrarMovimiento(50L, "INGRESO", BigDecimal.TEN, "x"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void registrarMovimiento_exitoso_admin_puedeOperarTurnoDeOtroUsuario() {
        when(cajaTurnoRepository.findById(50L)).thenReturn(Optional.of(turnoAbierto));
        when(currentUserProvider.obtenerOFallar()).thenReturn(admin);
        when(cajaMovimientoRepository.save(any(CajaMovimiento.class))).thenAnswer(inv -> {
            CajaMovimiento m = inv.getArgument(0);
            m.setId(70L);
            return m;
        });
        stubSinVentasNiMovimientos();

        CajaMovimientoDTO dto = service.registrarMovimiento(50L, "ingreso", new BigDecimal("25.00"), "Venta extra");

        assertThat(dto.getTipo()).isEqualTo("INGRESO");
        assertThat(dto.getMonto()).isEqualByComparingTo("25.00");
    }

    @Test
    void cierre_exitoso_calculaDiferencia() {
        when(cajaTurnoRepository.findById(50L)).thenReturn(Optional.of(turnoAbierto));
        when(currentUserProvider.obtenerOFallar()).thenReturn(empleado);
        stubSinVentasNiMovimientos();
        when(cajaTurnoRepository.save(any(CajaTurno.class))).thenAnswer(inv -> inv.getArgument(0));

        CajaTurnoDTO dto = service.cierre(50L, new BigDecimal("150.00"), "Todo cuadrado");

        assertThat(dto.getEstado()).isEqualTo("CERRADO");
        // teorico = montoApertura(100) + ventas(0) + ingresos(0) - egresos(0) = 100; diferencia = 150 - 100 = 50
        assertThat(dto.getDiferencia()).isEqualByComparingTo("50.00");
    }

    @Test
    void cierre_turnoYaCerrado_lanzaIllegalStateException() {
        turnoAbierto.setEstado("CERRADO");
        when(cajaTurnoRepository.findById(50L)).thenReturn(Optional.of(turnoAbierto));
        when(currentUserProvider.obtenerOFallar()).thenReturn(empleado);

        assertThatThrownBy(() -> service.cierre(50L, BigDecimal.TEN, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cierre_propietarioDistintoSinSerAdmin_lanzaAccessDenied() {
        Usuario otro = new Usuario();
        otro.setId(77L);
        otro.setRol(Rol.EMPLEADO);

        when(cajaTurnoRepository.findById(50L)).thenReturn(Optional.of(turnoAbierto));
        when(currentUserProvider.obtenerOFallar()).thenReturn(otro);

        assertThatThrownBy(() -> service.cierre(50L, BigDecimal.TEN, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cierre_notasConPassword_sanitizaAntesDeGuardar() {
        when(cajaTurnoRepository.findById(50L)).thenReturn(Optional.of(turnoAbierto));
        when(currentUserProvider.obtenerOFallar()).thenReturn(empleado);
        stubSinVentasNiMovimientos();
        when(cajaTurnoRepository.save(any(CajaTurno.class))).thenAnswer(inv -> inv.getArgument(0));

        CajaTurnoDTO dto = service.cierre(50L, BigDecimal.TEN, "acceso password=abc123 al sistema");

        assertThat(dto.getNotas()).contains("password=[PROTECTED]").doesNotContain("abc123");
    }

    @Test
    void listarHistorial_mapeaListaDeTurnos() {
        when(cajaTurnoRepository.findBySucursalIdOrderByFechaAperturaDesc(1L)).thenReturn(List.of(turnoAbierto));
        stubSinVentasNiMovimientos();

        List<CajaTurnoDTO> lista = service.listarHistorial(1L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getId()).isEqualTo(50L);
    }

    @Test
    void listarMovimientos_mapeaLista() {
        CajaMovimiento mov = new CajaMovimiento();
        mov.setId(80L);
        mov.setCajaTurno(turnoAbierto);
        mov.setTipo("EGRESO");
        mov.setMonto(new BigDecimal("15.00"));
        mov.setConcepto("Compra insumos");
        mov.setFecha(LocalDateTime.now());

        when(cajaMovimientoRepository.findByCajaTurnoIdOrderByFechaAsc(50L)).thenReturn(List.of(mov));

        List<CajaMovimientoDTO> lista = service.listarMovimientos(50L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getMonto()).isEqualByComparingTo("15.00");
    }
}
