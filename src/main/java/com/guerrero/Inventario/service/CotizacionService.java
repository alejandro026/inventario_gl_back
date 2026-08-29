package com.guerrero.Inventario.service;

import com.guerrero.Inventario.dto.CotizacionDTO;
import com.guerrero.Inventario.dto.DetalleCotizacionDTO;
import com.guerrero.Inventario.exception.BusinessException;
import com.guerrero.Inventario.exception.ResourceNotFoundException;
import com.guerrero.Inventario.mapper.CotizacionMapper;
import com.guerrero.Inventario.model.*;
import com.guerrero.Inventario.repository.*;
import com.guerrero.Inventario.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CotizacionService {

    private static final Logger log = LoggerFactory.getLogger(CotizacionService.class);

    private final CotizacionRepository repository;
    private final ProductoService productoService;
    private final SucursalService sucursalService;
    private final ClienteRepository clienteRepository;
    private final PromocionRepository promocionRepository;
    private final CurrentUserProvider currentUserProvider;

    public CotizacionService(CotizacionRepository repository,
                             ProductoService productoService,
                             SucursalService sucursalService,
                             ClienteRepository clienteRepository,
                             PromocionRepository promocionRepository,
                             CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.productoService = productoService;
        this.sucursalService = sucursalService;
        this.clienteRepository = clienteRepository;
        this.promocionRepository = promocionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<CotizacionDTO> listarPorSucursal(Long sucursalId) {
        // Ejecutar trigger/expiración lógica en cada consulta de listado para mantener consistencia
        expirarCotizacionesVencidas();
        return repository.findBySucursalIdOrderByFechaDesc(sucursalId).stream()
                .map(CotizacionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CotizacionDTO obtenerPorId(Long id) {
        expirarCotizacionesVencidas();
        Cotizacion c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización", id));
        return CotizacionMapper.toDto(c);
    }

    public CotizacionDTO guardar(CotizacionDTO dto) {
        Cotizacion cotizacion;
        LocalDateTime ahora = LocalDateTime.now();

        if (dto.getId() != null) {
            // Edición
            cotizacion = repository.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cotización", dto.getId()));

            if (cotizacion.getEstado() != Cotizacion.EstadoCotizacion.PENDIENTE) {
                throw new BusinessException("Solo se pueden editar cotizaciones en estado PENDIENTE.");
            }
            if (cotizacion.getFechaVencimiento().isBefore(ahora)) {
                cotizacion.setEstado(Cotizacion.EstadoCotizacion.VENCIDA);
                repository.save(cotizacion);
                throw new BusinessException("La cotización ha vencido y no puede ser modificada.");
            }
            // Limpiar detalles antiguos para reemplazarlos
            cotizacion.getDetalle().clear();
        } else {
            // Nueva
            cotizacion = new Cotizacion();
            cotizacion.setFecha(ahora);
            cotizacion.setEstado(Cotizacion.EstadoCotizacion.PENDIENTE);
        }

        // Asignar Sucursal
        Sucursal sucursal = sucursalService.buscar(dto.getIdSucursal());
        cotizacion.setSucursal(sucursal);

        // Asignar Usuario/Cajero actual
        Usuario usuario = currentUserProvider.obtenerONull();
        if (usuario == null) {
            throw new BusinessException("No hay un usuario autenticado en la sesión actual.");
        }
        cotizacion.setUsuario(usuario);

        // Asignar Cliente opcional
        if (dto.getIdCliente() != null) {
            Cliente cliente = clienteRepository.findById(dto.getIdCliente())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente", dto.getIdCliente()));
            cotizacion.setCliente(cliente);
        } else {
            cotizacion.setCliente(null);
        }

        // Definir validez (1 semana desde ahora)
        cotizacion.setFechaVencimiento(ahora.plusDays(7));

        // Procesar detalles
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        List<DetalleCotizacion> detalles = new ArrayList<>();

        if (dto.getDetalle() == null || dto.getDetalle().isEmpty()) {
            throw new BusinessException("La cotización debe contener al menos un producto.");
        }

        for (DetalleCotizacionDTO detDto : dto.getDetalle()) {
            Producto producto = productoService.buscar(detDto.getProductoId());
            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new BusinessException("El producto '" + producto.getNombre() + "' está inactivo.");
            }

            int cantidad = detDto.getCantProd();
            if (cantidad <= 0) {
                throw new BusinessException("La cantidad debe ser mayor a cero para: " + producto.getNombre());
            }

            BigDecimal precio = producto.getPrecio();
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));

            DetalleCotizacion detalle = DetalleCotizacion.builder()
                    .cotizacion(cotizacion)
                    .producto(producto)
                    .cantProd(cantidad)
                    .precio(precio)
                    .subtotal(subtotal)
                    .build();

            detalles.add(detalle);
            totalSubtotal = totalSubtotal.add(subtotal);
        }
        cotizacion.getDetalle().addAll(detalles);

        // Calcular descuentos según promociones vigentes
        BigDecimal subtotalAcumulado = totalSubtotal;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal totalFinal = subtotalAcumulado;

        List<Promocion> activePromos = promocionRepository.findActivePromotions(cotizacion.getFecha());
        if (!activePromos.isEmpty()) {
            Promocion promo = activePromos.get(0);
            
            if (promo.getCategorias() != null && !promo.getCategorias().isEmpty()) {
                java.util.Set<Long> eligibleCatIds = promo.getCategorias().stream()
                        .map(Categoria::getId)
                        .collect(Collectors.toSet());
                
                BigDecimal subtotalElegible = BigDecimal.ZERO;
                for (DetalleCotizacion d : cotizacion.getDetalle()) {
                    if (d.getProducto() != null && d.getProducto().getCategoria() != null) {
                        Long catId = d.getProducto().getCategoria().getId();
                        if (eligibleCatIds.contains(catId)) {
                            subtotalElegible = subtotalElegible.add(d.getSubtotal());
                        }
                    }
                }
                
                if (subtotalElegible.compareTo(promo.getCompraMinima()) >= 0) {
                    descuento = subtotalElegible.multiply(promo.getPorcentajeDescuento())
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    totalFinal = subtotalAcumulado.subtract(descuento);
                }
            } else {
                if (subtotalAcumulado.compareTo(promo.getCompraMinima()) >= 0) {
                    descuento = subtotalAcumulado.multiply(promo.getPorcentajeDescuento())
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    totalFinal = subtotalAcumulado.subtract(descuento);
                }
            }
        }

        cotizacion.setSubtotal(subtotalAcumulado);
        cotizacion.setDescuento(descuento);
        cotizacion.setTotal(totalFinal);

        log.info("Cotización guardada exitosamente. ID: {}, Total: {}, Estado: {}", 
                cotizacion.getId(), cotizacion.getTotal(), cotizacion.getEstado());

        Cotizacion saved = repository.save(cotizacion);
        return CotizacionMapper.toDto(saved);
    }

    public void registrarVentaLink(Long cotizacionId, Long ventaId) {
        Cotizacion c = repository.findById(cotizacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización", cotizacionId));
        
        if (c.getEstado() != Cotizacion.EstadoCotizacion.PENDIENTE) {
            throw new BusinessException("La cotización no se encuentra PENDIENTE.");
        }
        
        c.setEstado(Cotizacion.EstadoCotizacion.VENDIDA);
        c.setVentaId(ventaId);
        repository.save(c);
        log.info("Cotización ID: {} marcada como VENDIDA y vinculada a la venta ID: {}", cotizacionId, ventaId);
    }

    public void eliminar(Long id) {
        Cotizacion c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización", id));
        if (c.getEstado() == Cotizacion.EstadoCotizacion.VENDIDA) {
            throw new BusinessException("No se puede eliminar una cotización que ya ha sido convertida en venta.");
        }
        repository.delete(c);
        log.info("Cotización ID: {} eliminada exitosamente.", id);
    }

    private void expirarCotizacionesVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        repository.expirarCotizacionesVencidas(ahora);
    }
}
