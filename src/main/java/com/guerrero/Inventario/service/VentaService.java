package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.DetalleVentaDTO;
import com.guerrero.Inventario.dto.VentaDTO;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.InsufficientStockException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.mapper.VentaMapper;
import com.guerrero.Inventario.model.*;
import com.guerrero.Inventario.repository.*;
import com.guerrero.Inventario.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VentaService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VentaService.class);

    private final VentaRepository ventaRepository;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final KardexService kardexService;
    private final ClienteRepository clienteRepository;
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final CajaTurnoRepository cajaTurnoRepository;
    private final CurrentUserProvider currentUserProvider;

    public VentaService(VentaRepository ventaRepository,
                        ProductoService productoService,
                        SucursalService sucursalService,
                        KardexService kardexService,
                        ClienteRepository clienteRepository,
                        CuentaPorCobrarRepository cuentaPorCobrarRepository,
                        CajaTurnoRepository cajaTurnoRepository,
                        CurrentUserProvider currentUserProvider) {
        this.ventaRepository = ventaRepository;
        this.productoService = productoService;
        this.sucursalService = sucursalService;
        this.kardexService = kardexService;
        this.clienteRepository = clienteRepository;
        this.cuentaPorCobrarRepository = cuentaPorCobrarRepository;
        this.cajaTurnoRepository = cajaTurnoRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public Page<VentaDTO> listar(Pageable pageable, Long sucursalId, LocalDateTime inicio, LocalDateTime fin) {
        Page<Venta> page;
        if (inicio != null && fin != null) {
            page = sucursalId == null
                    ? ventaRepository.findByFechaBetweenOrderByFechaDesc(inicio, fin, pageable)
                    : ventaRepository.findBySucursal_IdAndFechaBetweenOrderByFechaDesc(sucursalId, inicio, fin, pageable);
        } else {
            page = sucursalId == null
                    ? ventaRepository.findAllByOrderByFechaDesc(pageable)
                    : ventaRepository.findBySucursal_IdOrderByFechaDesc(sucursalId, pageable);
        }
        return page.map(VentaMapper::toDto);
    }

    @Transactional(readOnly = true)
    public VentaDTO obtener(Long id) {
        return VentaMapper.toDto(buscar(id));
    }

    @Transactional(readOnly = true)
    public BigDecimal totalVendido(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || inicio.isAfter(fin)) {
            throw new BusinessException("Rango de fechas invalido");
        }
        BigDecimal total = ventaRepository.totalVendidoEntre(inicio, fin);
        return total == null ? BigDecimal.ZERO : total;
    }

    public VentaDTO registrar(VentaDTO dto) {
        log.info("Iniciando registro de venta. Sucursal ID: {}, Cliente ID: {}, Método Pago: {}, Total detalles: {}", 
                dto.getIdSucursal(), dto.getIdCliente(), dto.getMetodoPago(), dto.getDetalle() != null ? dto.getDetalle().size() : 0);
        Usuario usuario = currentUserProvider.obtenerONull();
        Sucursal sucursal = null;

        if (usuario != null && usuario.getSucursal() != null) {
            sucursal = usuario.getSucursal();
        } else if (dto.getIdSucursal() != null) {
            sucursal = sucursalService.buscar(dto.getIdSucursal());
        }

        if (sucursal == null) {
            throw new BusinessException("Debe especificar una sucursal para la venta.");
        }

        // Validar turno de caja abierto para el usuario en la sucursal
        if (usuario != null) {
            cajaTurnoRepository.findFirstBySucursalIdAndUsuarioIdAndEstadoOrderByFechaAperturaDesc(
                    sucursal.getId(), usuario.getId(), "ABIERTO")
                    .orElseThrow(() -> new BusinessException("Debe abrir un turno de caja para esta sucursal antes de realizar ventas."));
        }

        Venta venta = new Venta();
        venta.setSucursal(sucursal);
        venta.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDateTime.now());
        venta.setEstado(parseEstado(dto.getEstado(), Venta.EstadoVenta.COMPLETADA));
        venta.setUsuario(usuario);

        Venta.MetodoPago metodo = Venta.MetodoPago.EFECTIVO;
        if (dto.getMetodoPago() != null) {
            try {
                metodo = Venta.MetodoPago.valueOf(dto.getMetodoPago().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Método de pago inválido: " + dto.getMetodoPago());
            }
        }
        venta.setMetodoPago(metodo);

        Cliente cliente = null;
        if (dto.getIdCliente() != null) {
            cliente = clienteRepository.findById(dto.getIdCliente())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente", dto.getIdCliente()));
            venta.setCliente(cliente);
        }

        if (metodo == Venta.MetodoPago.CREDITO && cliente == null) {
            throw new BusinessException("Debe seleccionar un cliente para realizar una venta a crédito.");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<DetalleVentaDTO> detalles = dto.getDetalle();
        if (detalles == null || detalles.isEmpty()) {
            throw new BusinessException("La venta debe contener al menos un detalle");
        }

        for (DetalleVentaDTO d : detalles) {
            Producto producto = productoService.buscar(d.getProductoId());
            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new BusinessException("El producto '" + producto.getNombre() + "' esta inactivo");
            }
            int cantidad = d.getCantProd();
            if (cantidad <= 0) {
                throw new BusinessException("La cantidad debe ser positiva para el producto "
                        + producto.getNombre());
            }
            if (venta.getEstado() == Venta.EstadoVenta.COMPLETADA
                    && Boolean.TRUE.equals(producto.getControlaStock())
                    && producto.getCantidad() < cantidad) {
                throw new InsufficientStockException(
                        "Stock insuficiente para '" + producto.getNombre()
                                + "'. Disponible: " + producto.getCantidad()
                                + ", solicitado: " + cantidad);
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantProd(cantidad);
            detalle.setPrecio(producto.getPrecio());
            detalle.setPrecioCompra(producto.getPrecioCompra() != null ? producto.getPrecioCompra() : BigDecimal.ZERO);
            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
            detalle.setSubtotal(subtotal);
            venta.getDetalle().add(detalle);
            total = total.add(subtotal);

            if (venta.getEstado() == Venta.EstadoVenta.COMPLETADA && Boolean.TRUE.equals(producto.getControlaStock())) {
                producto.setCantidad(producto.getCantidad() - cantidad);
            }
        }

        venta.setTotal(total);
        if (metodo == Venta.MetodoPago.CREDITO) {
            BigDecimal saldoActual = cliente.getSaldoPendiente() != null ? cliente.getSaldoPendiente() : BigDecimal.ZERO;
            BigDecimal nuevoSaldo = saldoActual.add(total);
            if (cliente.getLimiteCredito() != null && nuevoSaldo.compareTo(cliente.getLimiteCredito()) > 0) {
                throw new BusinessException("Límite de crédito excedido. Disponible: $"
                        + cliente.getLimiteCredito().subtract(saldoActual)
                        + ", Total venta: $" + total);
            }
            cliente.setSaldoPendiente(nuevoSaldo);
            clienteRepository.save(cliente);
            venta.setPagoCon(BigDecimal.ZERO);
            venta.setCambio(BigDecimal.ZERO);
        } else {
            if (dto.getPagoCon() != null && dto.getPagoCon().compareTo(BigDecimal.ZERO) > 0) {
                venta.setPagoCon(dto.getPagoCon());
                venta.setCambio(dto.getPagoCon().subtract(total).max(BigDecimal.ZERO));
            } else {
                venta.setPagoCon(total);
                venta.setCambio(BigDecimal.ZERO);
            }
        }

        Venta guardada = ventaRepository.save(venta);
        log.info("Venta ID: {} registrada con éxito. Sucursal ID: {}, Cliente ID: {}, Total: {}, Método Pago: {}", 
                guardada.getId(), guardada.getSucursal().getId(), guardada.getCliente() != null ? guardada.getCliente().getId() : "Público General", guardada.getTotal(), guardada.getMetodoPago());

        // Registrar movimientos en Kardex
        for (DetalleVenta d : guardada.getDetalle()) {
            Producto producto = d.getProducto();
            if (guardada.getEstado() == Venta.EstadoVenta.COMPLETADA && Boolean.TRUE.equals(producto.getControlaStock())) {
                log.trace("Registrando salida en Kardex para producto ID: {}", producto.getId());
                kardexService.registrarMovimiento(
                        producto,
                        guardada.getSucursal(),
                        "SALIDA",
                        d.getCantProd(),
                        "VENTA",
                        usuario,
                        guardada.getId()
                );
            }
        }

        // Generar cuenta por cobrar si es a crédito
        if (metodo == Venta.MetodoPago.CREDITO) {
            CuentaPorCobrar cxc = new CuentaPorCobrar();
            cxc.setCliente(cliente);
            cxc.setVenta(guardada);
            cxc.setMontoTotal(total);
            cxc.setSaldoPendiente(total);
            cxc.setEstado("PENDIENTE");
            cuentaPorCobrarRepository.save(cxc);
        }

        return VentaMapper.toDto(guardada);
    }

    public VentaDTO cancelar(Long id) {
        log.info("Procesando cancelación de venta para ID: {}", id);
        Venta v = buscar(id);
        if (v.getEstado() == Venta.EstadoVenta.CANCELADA) {
            log.warn("Cancelación fallida: La venta ID: {} ya está CANCELADA.", id);
            throw new BusinessException("La venta ya esta cancelada");
        }
        // Reintegrar el stock si la venta estaba completada
        if (v.getEstado() == Venta.EstadoVenta.COMPLETADA) {
            for (DetalleVenta d : v.getDetalle()) {
                Producto p = d.getProducto();
                if (p != null && Boolean.TRUE.equals(p.getControlaStock())) {
                    p.setCantidad(p.getCantidad() + d.getCantProd());
                    kardexService.registrarMovimiento(
                            p,
                            v.getSucursal(),
                            "ENTRADA",
                            d.getCantProd(),
                            "CANCELACION",
                            currentUserProvider.obtenerONull(),
                            v.getId()
                      );
                }
            }
        }

        // Revertir deudas de clientes
        if (v.getMetodoPago() == Venta.MetodoPago.CREDITO && v.getCliente() != null) {
            List<CuentaPorCobrar> cxcs = cuentaPorCobrarRepository.findByClienteId(v.getCliente().getId());
            CuentaPorCobrar cxcAsociada = cxcs.stream()
                    .filter(cxc -> cxc.getVenta() != null && cxc.getVenta().getId().equals(v.getId()))
                    .findFirst().orElse(null);
            if (cxcAsociada != null) {
                Cliente cliente = v.getCliente();
                BigDecimal saldoARestar = cxcAsociada.getSaldoPendiente();
                BigDecimal saldoClienteActual = cliente.getSaldoPendiente() != null ? cliente.getSaldoPendiente() : BigDecimal.ZERO;
                cliente.setSaldoPendiente(saldoClienteActual.subtract(saldoARestar).max(BigDecimal.ZERO));
                clienteRepository.save(cliente);

                cxcAsociada.setSaldoPendiente(BigDecimal.ZERO);
                cxcAsociada.setEstado("CANCELADA");
                cuentaPorCobrarRepository.save(cxcAsociada);
            }
        }

        v.setEstado(Venta.EstadoVenta.CANCELADA);
        Venta guardada = ventaRepository.save(v);
        log.info("Venta ID: {} cancelada exitosamente. Se restableció el stock y saldos de cuentas correspondientes.", id);
        return VentaMapper.toDto(guardada);
    }

    public Venta buscar(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", id));
    }

    private Venta.EstadoVenta parseEstado(String raw, Venta.EstadoVenta porDefecto) {
        if (raw == null || raw.isBlank()) return porDefecto;
        try {
            return Venta.EstadoVenta.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Estado de venta invalido: " + raw);
        }
    }

}
