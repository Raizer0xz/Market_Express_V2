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

/**
 * InventarioService: logica de negocio para el inventario.
 *
 * Permisos por operacion:
 *   verStock()         → todos los roles (el controller no filtra)
 *   aumentar()         → solo ADMIN (validado en el controller)
 *   reducir()          → solo ADMIN (validado en el controller)
 *   obtenerAlertas()   → global, sin filtro de rol
 *   historial()        → solo ADMIN (validado en el controller)
 *
 * Cada cambio de stock genera automaticamente un MovimientoInventario
 * para mantener el historial de auditoria completo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    // ─────────────────────────────────────────────────────────────
    // CONSULTAS DE STOCK (todos los roles)
    // ─────────────────────────────────────────────────────────────

    /**
     * Obtiene el stock de un producto en una sucursal especifica.
     * Disponible para todos los roles.
     */
    public Inventario verStock(Long productoId, Long sucursalId) {
        return inventarioRepository.findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElseThrow(() -> new RuntimeException(
                        "No hay inventario para producto " + productoId
                        + " en sucursal " + sucursalId));
    }

    /**
     * Lista todo el inventario de una sucursal.
     * Util para que el admin de sucursal vea todo su stock de un vistazo.
     */
    public List<Inventario> verStockPorSucursal(Long sucursalId) {
        return inventarioRepository.findBySucursalId(sucursalId);
    }

    /**
     * Lista todas las sucursales que tienen un producto determinado.
     * Util para saber disponibilidad global de un producto.
     */
    public List<Inventario> verStockPorProducto(Long productoId) {
        return inventarioRepository.findByProductoId(productoId);
    }

    // ─────────────────────────────────────────────────────────────
    // ALERTAS DE STOCK MINIMO (visibles globalmente)
    // ─────────────────────────────────────────────────────────────

    /**
     * Retorna todos los productos en todas las sucursales
     * cuyo stock actual es menor o igual al stock minimo configurado.
     * Visible para todos los roles.
     */
    public List<Inventario> obtenerAlertasGlobales() {
        log.info("Consultando alertas de stock bajo globales");
        return inventarioRepository.findStockBajo();
    }

    /**
     * Retorna alertas de stock bajo solo para una sucursal especifica.
     */
    public List<Inventario> obtenerAlertasPorSucursal(Long sucursalId) {
        return inventarioRepository.findStockBajoPorSucursal(sucursalId);
    }

    // ─────────────────────────────────────────────────────────────
    // MODIFICACIONES DE STOCK (solo ADMIN)
    // ─────────────────────────────────────────────────────────────

    /**
     * Aumenta el stock de un producto en una sucursal.
     * Si no existe el registro de inventario, lo crea automaticamente.
     * Solo ADMIN puede llamar este metodo.
     *
     * @param productoId  ID del producto
     * @param sucursalId  ID de la sucursal
     * @param cantidad    Cantidad a agregar (debe ser > 0)
     * @param motivo      Descripcion del motivo (ej: "Recepcion de proveedor X")
     * @param adminId     ID del admin que realiza el movimiento
     */
    @Transactional
    public Inventario aumentarStock(Long productoId, Long sucursalId,
                                    Integer cantidad, String motivo, Long adminId) {
        if (cantidad <= 0) throw new RuntimeException("La cantidad debe ser mayor a 0");

        // Buscar o crear el registro de inventario
        Inventario inv = inventarioRepository
                .findByProductoIdAndSucursalId(productoId, sucursalId)
                .orElse(Inventario.builder()
                        .productoId(productoId)
                        .sucursalId(sucursalId)
                        .cantidad(0)
                        .stockMinimo(5)
                        .build());

        int cantidadAnterior = inv.getCantidad();
        inv.setCantidad(cantidadAnterior + cantidad);
        inventarioRepository.save(inv);

        // Registrar el movimiento en el historial
        registrarMovimiento(productoId, sucursalId, TipoMovimiento.ENTRADA,
                cantidad, inv.getCantidad(), motivo, adminId);

        log.info("ENTRADA: producto={} sucursal={} cantidad=+{} total={}",
                productoId, sucursalId, cantidad, inv.getCantidad());

        return inv;
    }

    /**
     * Reduce el stock de un producto en una sucursal.
     * Lanza excepcion si no hay suficiente stock disponible.
     * Solo ADMIN puede llamar este metodo.
     *
     * @param productoId  ID del producto
     * @param sucursalId  ID de la sucursal
     * @param cantidad    Cantidad a descontar (debe ser > 0)
     * @param motivo      Descripcion del motivo (ej: "Pedido #42")
     * @param adminId     ID del admin que realiza el movimiento
     */
    @Transactional
    public Inventario reducirStock(Long productoId, Long sucursalId,
                                   Integer cantidad, String motivo, Long adminId) {
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

        // Alerta en log si cayo bajo el minimo
        if (inv.getCantidad() <= inv.getStockMinimo()) {
            log.warn("ALERTA STOCK BAJO: producto={} sucursal={} cantidad={}",
                    productoId, sucursalId, inv.getCantidad());
        }

        return inv;
    }

    /**
     * Ajusta el stock directamente a un valor exacto.
     * Util para correcciones despues de conteos fisicos.
     * Solo ADMIN.
     *
     * @param productoId    ID del producto
     * @param sucursalId    ID de la sucursal
     * @param nuevaCantidad Cantidad final correcta
     * @param motivo        Motivo del ajuste (ej: "Conteo fisico mensual")
     * @param adminId       ID del admin que realiza el ajuste
     */
    @Transactional
    public Inventario ajustarStock(Long productoId, Long sucursalId,
                                   Integer nuevaCantidad, String motivo, Long adminId) {
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

    /**
     * Actualiza el umbral de stock minimo de un producto en una sucursal.
     * Cuando la cantidad cae por debajo de este umbral, aparece en alertas.
     */
    @Transactional
    public Inventario actualizarStockMinimo(Long productoId, Long sucursalId, Integer nuevoMinimo) {
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

    /**
     * Historial completo de movimientos de un producto en una sucursal.
     * Ordenado de mas reciente a mas antiguo.
     * Solo ADMIN.
     */
    public List<MovimientoInventario> historialPorProductoYSucursal(Long productoId, Long sucursalId) {
        return movimientoRepository
                .findByProductoIdAndSucursalIdOrderByCreatedAtDesc(productoId, sucursalId);
    }

    /**
     * Historial completo de todos los movimientos de una sucursal.
     * Solo ADMIN.
     */
    public List<MovimientoInventario> historialPorSucursal(Long sucursalId) {
        return movimientoRepository.findBySucursalIdOrderByCreatedAtDesc(sucursalId);
    }

    /**
     * Historial filtrado por tipo de movimiento en una sucursal.
     * Ej: ver solo las ENTRADAS o solo las SALIDAS.
     * Solo ADMIN.
     */
    public List<MovimientoInventario> historialPorTipo(Long sucursalId, TipoMovimiento tipo) {
        return movimientoRepository.findBySucursalIdAndTipoOrderByCreatedAtDesc(sucursalId, tipo);
    }

    // ─────────────────────────────────────────────────────────────
    // METODO INTERNO: registrar movimiento
    // ─────────────────────────────────────────────────────────────

    /** Guarda un registro en el historial de movimientos. Uso interno del servicio. */
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
