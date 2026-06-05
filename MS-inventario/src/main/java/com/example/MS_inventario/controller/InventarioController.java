package com.example.MS_inventario.controller;

import com.example.MS_inventario.model.Inventario;
import com.example.MS_inventario.model.MovimientoInventario;
import com.example.MS_inventario.model.TipoMovimiento;
import com.example.MS_inventario.service.InventarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * InventarioController: endpoints REST del MS-inventario (puerto 8086).
 *
 * ── PUBLICOS (todos los roles) ──────────────────────────────────────────────
 * GET /api/v1/inventario/sucursal/{sucursalId}
 *      → Todo el stock de una sucursal
 *
 * GET /api/v1/inventario/producto/{productoId}
 *      → Stock de un producto en todas las sucursales
 *
 * GET /api/v1/inventario/producto/{productoId}/sucursal/{sucursalId}
 *      → Stock de un producto en una sucursal especifica
 *
 * GET /api/v1/inventario/alertas
 *      → Todos los productos con stock bajo en todo el sistema
 *
 * GET /api/v1/inventario/alertas/sucursal/{sucursalId}
 *      → Productos con stock bajo en una sucursal especifica
 *
 * ── SOLO ADMIN (requiere header X-Usuario-Rol: ADMIN) ──────────────────────
 * POST /api/v1/inventario/aumentar
 *      → Aumentar stock (entrada de mercaderia)
 *
 * POST /api/v1/inventario/reducir
 *      → Reducir stock (salida / pedido)
 *
 * POST /api/v1/inventario/ajustar
 *      → Ajustar stock a un valor exacto
 *
 * PUT  /api/v1/inventario/stock-minimo
 *      → Cambiar el umbral de alerta de un producto
 *
 * GET  /api/v1/inventario/historial/sucursal/{sucursalId}
 *      → Historial de todos los movimientos de una sucursal
 *
 * GET  /api/v1/inventario/historial/producto/{productoId}/sucursal/{sucursalId}
 *      → Historial de un producto en una sucursal
 *
 * GET  /api/v1/inventario/historial/sucursal/{sucursalId}/tipo/{tipo}
 *      → Historial filtrado por tipo (ENTRADA | SALIDA | AJUSTE)
 *
 * NOTA sobre autenticacion:
 *   Los endpoints de ADMIN esperan el header X-Usuario-Rol: ADMIN
 *   y X-Usuario-Id: <id del admin>. En produccion esto lo envia
 *   el Gateway tras validar el JWT con MS-seguridad.
 */
@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService service;

    // ─────────────────────────────────────────────────────────────
    // CONSULTAS DE STOCK (todos los roles)
    // ─────────────────────────────────────────────────────────────

    /** Todo el stock de una sucursal */
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<Inventario>> porSucursal(@PathVariable Long sucursalId) {
        return ResponseEntity.ok(service.verStockPorSucursal(sucursalId));
    }

    /** Stock de un producto en todas las sucursales */
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<Inventario>> porProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.verStockPorProducto(productoId));
    }

    /** Stock de un producto en una sucursal especifica */
    @GetMapping("/producto/{productoId}/sucursal/{sucursalId}")
    public ResponseEntity<?> stockExacto(
            @PathVariable Long productoId,
            @PathVariable Long sucursalId) {
        try {
            return ResponseEntity.ok(service.verStock(productoId, sucursalId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ALERTAS (visibles globalmente)
    // ─────────────────────────────────────────────────────────────

    /** Todos los productos con stock bajo en todo el sistema */
    @GetMapping("/alertas")
    public ResponseEntity<List<Inventario>> alertasGlobales() {
        return ResponseEntity.ok(service.obtenerAlertasGlobales());
    }

    /** Productos con stock bajo en una sucursal especifica */
    @GetMapping("/alertas/sucursal/{sucursalId}")
    public ResponseEntity<List<Inventario>> alertasPorSucursal(@PathVariable Long sucursalId) {
        return ResponseEntity.ok(service.obtenerAlertasPorSucursal(sucursalId));
    }

    // ─────────────────────────────────────────────────────────────
    // MODIFICACIONES DE STOCK (solo ADMIN)
    // ─────────────────────────────────────────────────────────────

    /**
     * Aumenta el stock de un producto en una sucursal.
     * Body esperado:
     * {
     *   "productoId": 1,
     *   "sucursalId": 2,
     *   "cantidad": 50,
     *   "motivo": "Recepcion proveedor X"
     * }
     */
    @PostMapping("/aumentar")
    public ResponseEntity<?> aumentar(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "0") Long adminId,
            @RequestBody Map<String, Object> body) {

        if (!esAdmin(rol)) return sinPermiso();

        try {
            Long productoId = getLong(body, "productoId");
            Long sucursalId = getLong(body, "sucursalId");
            Integer cantidad = getInt(body, "cantidad");
            String motivo = (String) body.getOrDefault("motivo", "Sin motivo");

            return ResponseEntity.ok(service.aumentarStock(productoId, sucursalId, cantidad, motivo, adminId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reduce el stock de un producto en una sucursal.
     * Body esperado:
     * {
     *   "productoId": 1,
     *   "sucursalId": 2,
     *   "cantidad": 10,
     *   "motivo": "Pedido #42"
     * }
     */
    @PostMapping("/reducir")
    public ResponseEntity<?> reducir(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "0") Long adminId,
            @RequestBody Map<String, Object> body) {

        if (!esAdmin(rol)) return sinPermiso();

        try {
            Long productoId = getLong(body, "productoId");
            Long sucursalId = getLong(body, "sucursalId");
            Integer cantidad = getInt(body, "cantidad");
            String motivo = (String) body.getOrDefault("motivo", "Sin motivo");

            return ResponseEntity.ok(service.reducirStock(productoId, sucursalId, cantidad, motivo, adminId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Ajusta el stock a un valor exacto (conteo fisico).
     * Body esperado:
     * {
     *   "productoId": 1,
     *   "sucursalId": 2,
     *   "nuevaCantidad": 35,
     *   "motivo": "Conteo fisico mensual"
     * }
     */
    @PostMapping("/ajustar")
    public ResponseEntity<?> ajustar(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "0") Long adminId,
            @RequestBody Map<String, Object> body) {

        if (!esAdmin(rol)) return sinPermiso();

        try {
            Long productoId = getLong(body, "productoId");
            Long sucursalId = getLong(body, "sucursalId");
            Integer nuevaCantidad = getInt(body, "nuevaCantidad");
            String motivo = (String) body.getOrDefault("motivo", "Ajuste manual");

            return ResponseEntity.ok(service.ajustarStock(productoId, sucursalId, nuevaCantidad, motivo, adminId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Actualiza el umbral de alerta de stock minimo.
     * Params: productoId, sucursalId, stockMinimo (query params)
     */
    @PutMapping("/stock-minimo")
    public ResponseEntity<?> actualizarStockMinimo(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @RequestParam Long productoId,
            @RequestParam Long sucursalId,
            @RequestParam Integer stockMinimo) {

        if (!esAdmin(rol)) return sinPermiso();

        try {
            return ResponseEntity.ok(service.actualizarStockMinimo(productoId, sucursalId, stockMinimo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HISTORIAL (solo ADMIN)
    // ─────────────────────────────────────────────────────────────

    /** Historial completo de una sucursal */
    @GetMapping("/historial/sucursal/{sucursalId}")
    public ResponseEntity<?> historialSucursal(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @PathVariable Long sucursalId) {

        if (!esAdmin(rol)) return sinPermiso();
        return ResponseEntity.ok(service.historialPorSucursal(sucursalId));
    }

    /** Historial de un producto en una sucursal */
    @GetMapping("/historial/producto/{productoId}/sucursal/{sucursalId}")
    public ResponseEntity<?> historialProducto(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @PathVariable Long productoId,
            @PathVariable Long sucursalId) {

        if (!esAdmin(rol)) return sinPermiso();
        return ResponseEntity.ok(service.historialPorProductoYSucursal(productoId, sucursalId));
    }

    /** Historial filtrado por tipo de movimiento (ENTRADA | SALIDA | AJUSTE) */
    @GetMapping("/historial/sucursal/{sucursalId}/tipo/{tipo}")
    public ResponseEntity<?> historialPorTipo(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @PathVariable Long sucursalId,
            @PathVariable TipoMovimiento tipo) {

        if (!esAdmin(rol)) return sinPermiso();
        return ResponseEntity.ok(service.historialPorTipo(sucursalId, tipo));
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────

    /** Verifica si el rol del header es ADMIN */
    private boolean esAdmin(String rol) {
        return "ADMIN".equalsIgnoreCase(rol);
    }

    /** Respuesta estandar para acceso denegado */
    private ResponseEntity<Map<String, String>> sinPermiso() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Acceso denegado. Se requiere rol ADMIN"));
    }

    private Long getLong(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) throw new RuntimeException("Falta el campo: " + key);
        return Long.valueOf(val.toString());
    }

    private Integer getInt(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val == null) throw new RuntimeException("Falta el campo: " + key);
        return Integer.valueOf(val.toString());
    }
}
