package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.AbonoCreditoDTO;
import com.guerrero.Inventario.dto.ClienteDTO;
import com.guerrero.Inventario.dto.CuentaPorCobrarDTO;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.model.AbonoCredito;
import com.guerrero.Inventario.model.Cliente;
import com.guerrero.Inventario.model.CuentaPorCobrar;
import com.guerrero.Inventario.repository.AbonoCreditoRepository;
import com.guerrero.Inventario.repository.ClienteRepository;
import com.guerrero.Inventario.repository.CuentaPorCobrarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final AbonoCreditoRepository abonoCreditoRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          CuentaPorCobrarRepository cuentaPorCobrarRepository,
                          AbonoCreditoRepository abonoCreditoRepository) {
        this.clienteRepository = clienteRepository;
        this.cuentaPorCobrarRepository = cuentaPorCobrarRepository;
        this.abonoCreditoRepository = abonoCreditoRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> buscarPorNombre(String query) {
        return clienteRepository.findByNombreContainingIgnoreCase(query).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClienteDTO obtener(Long id) {
        return toDto(buscar(id));
    }

    public ClienteDTO crear(ClienteDTO dto) {
        Cliente c = new Cliente();
        c.setNombre(dto.getNombre());
        c.setTelefono(dto.getTelefono());
        c.setEmail(dto.getEmail());
        c.setDireccion(dto.getDireccion());
        c.setLimiteCredito(dto.getLimiteCredito() != null ? dto.getLimiteCredito() : BigDecimal.ZERO);
        c.setSaldoPendiente(BigDecimal.ZERO);
        return toDto(clienteRepository.save(c));
    }

    public ClienteDTO actualizar(Long id, ClienteDTO dto) {
        Cliente c = buscar(id);
        c.setNombre(dto.getNombre());
        c.setTelefono(dto.getTelefono());
        c.setEmail(dto.getEmail());
        c.setDireccion(dto.getDireccion());
        c.setLimiteCredito(dto.getLimiteCredito() != null ? dto.getLimiteCredito() : BigDecimal.ZERO);
        return toDto(clienteRepository.save(c));
    }

    public void eliminar(Long id) {
        Cliente c = buscar(id);
        clienteRepository.delete(c);
    }

    public Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    @Transactional(readOnly = true)
    public List<CuentaPorCobrarDTO> obtenerCuentasPorCobrar(Long clienteId) {
        return cuentaPorCobrarRepository.findByClienteId(clienteId).stream()
                .map(this::toCxcDto)
                .collect(Collectors.toList());
    }

    public AbonoCreditoDTO registrarAbono(Long cuentaId, BigDecimal monto, String metodoPago) {
        CuentaPorCobrar cxc = cuentaPorCobrarRepository.findById(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException("CuentaPorCobrar", cuentaId));

        if ("PAGADO".equals(cxc.getEstado())) {
            throw new IllegalStateException("Esta deuda ya ha sido pagada en su totalidad");
        }

        BigDecimal nuevoSaldo = cxc.getSaldoPendiente().subtract(monto);
        cxc.setSaldoPendiente(nuevoSaldo.max(BigDecimal.ZERO));
        if (cxc.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
            cxc.setEstado("PAGADO");
        }

        AbonoCredito abono = new AbonoCredito();
        abono.setCuentaPorCobrar(cxc);
        abono.setMonto(monto);
        abono.setFecha(LocalDateTime.now());
        abono.setMetodoPago(metodoPago != null ? metodoPago.toUpperCase() : "EFECTIVO");

        AbonoCredito savedAbono = abonoCreditoRepository.save(abono);

        // Update total outstanding balance for customer
        Cliente cliente = cxc.getCliente();
        BigDecimal saldoCliente = cliente.getSaldoPendiente().subtract(monto);
        cliente.setSaldoPendiente(saldoCliente.max(BigDecimal.ZERO));
        clienteRepository.save(cliente);

        cuentaPorCobrarRepository.save(cxc);

        return toAbonoDto(savedAbono);
    }

    @Transactional(readOnly = true)
    public List<AbonoCreditoDTO> obtenerHistorialAbonos(Long cuentaId) {
        return abonoCreditoRepository.findByCuentaPorCobrarIdOrderByFechaDesc(cuentaId).stream()
                .map(this::toAbonoDto)
                .collect(Collectors.toList());
    }

    private ClienteDTO toDto(Cliente c) {
        if (c == null) return null;
        return new ClienteDTO(
                c.getId(),
                c.getNombre(),
                c.getTelefono(),
                c.getEmail(),
                c.getDireccion(),
                c.getLimiteCredito(),
                c.getSaldoPendiente()
        );
    }

    private CuentaPorCobrarDTO toCxcDto(CuentaPorCobrar cxc) {
        if (cxc == null) return null;
        return new CuentaPorCobrarDTO(
                cxc.getId(),
                cxc.getCliente().getId(),
                cxc.getCliente().getNombre(),
                cxc.getVenta().getId(),
                cxc.getVenta().getFecha(),
                cxc.getMontoTotal(),
                cxc.getSaldoPendiente(),
                cxc.getEstado()
        );
    }

    private AbonoCreditoDTO toAbonoDto(AbonoCredito a) {
        if (a == null) return null;
        return new AbonoCreditoDTO(
                a.getId(),
                a.getCuentaPorCobrar().getId(),
                a.getMonto(),
                a.getFecha(),
                a.getMetodoPago()
        );
    }
}
