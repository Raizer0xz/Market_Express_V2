package com.example.MS_inventario.service;

import com.example.MS_inventario.model.Inventario;
import com.example.MS_inventario.model.MovimientoInventario;
import com.example.MS_inventario.model.TipoMovimiento;
import com.example.MS_inventario.repository.InventarioRepository;
import com.example.MS_inventario.repository.MovimientoInventarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    // ─────────────────────────────────────────────────────────────
    // HELPER: validación de rol ADMIN centralizada aquí
    // ─────────────────────────────────────────────────────────────
    private void validarAdmin(String rol) {
        if (!"ADMIN".equalsIgnoreCase(rol)) {
            throw new SecurityException("Acceso denegado. Se requiere rol ADMIN");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CONSULTAS DE STOCK (todos los roles)
    // ─────────────────────────────────────────────────────────────
    public Inventario verStock(Long productoId, Long sucursalId) {
        return inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new RuntimeException(
                        "No hay inventario para producto " + productoId
                                + " en sucursal " + sucursalId));
    }

    public List<Inventario> verStockPorSucursal(Long sucursalId) {
        return inventarioRepository.findBySucursalId(sucursalId);
    }

    public List<Inventario> verStockPorProducto(Long productoId) {
        return inventarioRepository.findByProductoId(productoId);
    }

    // ─────────────────────────────────────────────────────────────
    // ALERTAS (visibles globalmente)
    // ─────────────────────────────────────────────────────────────
    public List<Inventario> obtenerAlertasGlobales() {
        log.info("Consultando alertas de stock bajo globales");
        return inventarioRepository.findStockBajo();
    }

    public List<Inventario> obtenerAlertasPorSucursal(Long sucursalId) {
        return inventarioRepository.findStockBajoPorSucursal(sucursalId);
    }

    // ─────────────────────────────────────────────────────────────
    // MODIFICACIONES DE STOCK (solo ADMIN)
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public Inventario aumentarStock(Long productoId, Long sucursalId,
                                    Integer cantidad, String motivo, Long adminId, String rol) {
        // REGLA DE NEGOCIO: solo ADMIN puede modificar stock
        validarAdmin(rol);

        if (cantidad <= 0) throw new RuntimeException("La cantidad debe ser mayor a 0");

        Inventario inv = inventarioRepository
                .findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElse(Inventario.builder()
                        .productoId(productoId)
                        .sucursalId(sucursalId)
                        .cantidad(0)
                        .stockMinimo(5)
                        .build());

        inv.setCantidad(inv.getCantidad() + cantidad);
        inventarioRepository.save(inv);

        registrarMovimiento(productoId, sucursalId, TipoMovimiento.ENTRADA,
                cantidad, inv.getCantidad(), motivo, adminId);

        log.info("ENTRADA: producto={} sucursal={} cantidad=+{} total={}",
                productoId, sucursalId, cantidad, inv.getCantidad());
        return inv;
    }

    @Transactional
    public Inventario reducirStock(Long productoId, Long sucursalId,
                                   Integer cantidad, String motivo, Long adminId, String rol) {
        // REGLA DE NEGOCIO: solo ADMIN puede modificar stock
        validarAdmin(rol);

        if (cantidad <= 0) throw new RuntimeException("La cantidad debe ser mayor a 0");

        Inventario inv = inventarioRepository
                .findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new RuntimeException(
                        "No hay inventario para ese producto en esa sucursal"));

        if (inv.getCantidad() < cantidad) {
            throw new RuntimeException(
                    "Stock insuficiente. Disponible: " + inv.getCantidad()
                            + ", solicitado: " + cantidad);
        }

        inv.setCantidad(inv.getCantidad() - cantidad);
        inventarioRepository.save(inv);

        registrarMovimiento(productoId, sucursalId, TipoMovimiento.SALIDA,
                cantidad, inv.getCantidad(), motivo, adminId);

        log.info("SALIDA: producto={} sucursal={} cantidad=-{} total={}",
                productoId, sucursalId, cantidad, inv.getCantidad());

        if (inv.getCantidad() <= inv.getStockMinimo()) {
            log.warn("ALERTA STOCK BAJO: producto={} sucursal={} cantidad={}",
                    productoId, sucursalId, inv.getCantidad());
        }
        return inv;
    }

    @Transactional
    public Inventario ajustarStock(Long productoId, Long sucursalId,
                                   Integer nuevaCantidad, String motivo, Long adminId, String rol) {
        // REGLA DE NEGOCIO: solo ADMIN puede ajustar stock
        validarAdmin(rol);

        if (nuevaCantidad < 0) throw new RuntimeException("La cantidad no puede ser negativa");

        Inventario inv = inventarioRepository
                .findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new RuntimeException(
                        "No hay inventario para ese producto en esa sucursal"));

        int diferencia = Math.abs(nuevaCantidad - inv.getCantidad());
        inv.setCantidad(nuevaCantidad);
        inventarioRepository.save(inv);

        registrarMovimiento(productoId, sucursalId, TipoMovimiento.AJUSTE,
                diferencia, nuevaCantidad, motivo, adminId);

        log.info("AJUSTE: producto={} sucursal={} nuevaCantidad={}", productoId, sucursalId, nuevaCantidad);
        return inv;
    }

    @Transactional
    public Inventario actualizarStockMinimo(Long productoId, Long sucursalId,
                                            Integer nuevoMinimo, String rol) {
        // REGLA DE NEGOCIO: solo ADMIN puede cambiar stock mínimo
        validarAdmin(rol);

        if (nuevoMinimo < 0) throw new RuntimeException("El stock minimo no puede ser negativo");

        Inventario inv = inventarioRepository
                .findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new RuntimeException(
                        "No hay inventario para ese producto en esa sucursal"));

        inv.setStockMinimo(nuevoMinimo);
        return inventarioRepository.save(inv);
    }

    // ─────────────────────────────────────────────────────────────
    // HISTORIAL (solo ADMIN)
    // ─────────────────────────────────────────────────────────────
    public List<MovimientoInventario> historialPorProductoYSucursal(Long productoId,
                                                                    Long sucursalId, String rol) {
        validarAdmin(rol);
        return movimientoRepository
                .findByProductoIdAndSucursalIdOrderByCreatedAtDesc(productoId, sucursalId);
    }

    public List<MovimientoInventario> historialPorSucursal(Long sucursalId, String rol) {
        validarAdmin(rol);
        return movimientoRepository.findBySucursalIdOrderByCreatedAtDesc(sucursalId);
    }

    public List<MovimientoInventario> historialPorTipo(Long sucursalId,
                                                       TipoMovimiento tipo, String rol) {
        validarAdmin(rol);
        return movimientoRepository.findBySucursalIdAndTipoOrderByCreatedAtDesc(sucursalId, tipo);
    }

    // ─────────────────────────────────────────────────────────────
    // INTERNO: registrar movimiento
    // ─────────────────────────────────────────────────────────────
    private void registrarMovimiento(Long productoId, Long sucursalId, TipoMovimiento tipo,
                                     Integer cantidad, Integer stockResultante,
                                     String motivo, Long usuarioId) {
        MovimientoInventario mov = MovimientoInventario.builder()
                .productoId(productoId)
                .sucursalId(sucursalId)
                .tipo(tipo)
                .cantidad(cantidad)
                .stockResultante(stockResultante)
                .motivo(motivo)
                .usuarioId(usuarioId)
                .build();
        movimientoRepository.save(mov);
    }
}