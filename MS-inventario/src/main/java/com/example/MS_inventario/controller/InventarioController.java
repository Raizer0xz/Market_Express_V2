package com.example.MS_inventario.controller;

import com.example.MS_inventario.model.Inventario;
import com.example.MS_inventario.model.MovimientoInventario;
import com.example.MS_inventario.model.TipoMovimiento;
import com.example.MS_inventario.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventario", description = "Gestión de stock e inventario por sucursal")
@SecurityRequirement(name = "bearerAuth")
public class InventarioController {

    private final InventarioService service;

    // ─────────────────────────────────────────────────────────────
    // CONSULTAS DE STOCK (todos los roles)
    // ─────────────────────────────────────────────────────────────
    @Operation(summary = "Ver todo el stock de una sucursal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de inventario retornada"),
            @ApiResponse(responseCode = "204", description = "Sin registros para esa sucursal")
    })
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<CollectionModel<Inventario>> porSucursal(
            @Parameter(description = "ID de la sucursal", example = "1")
            @PathVariable Long sucursalId) {
        List<Inventario> lista = service.verStockPorSucursal(sucursalId);
        if (lista.isEmpty()) return ResponseEntity.noContent().build();
        lista.forEach(this::agregarLinks);
        return ResponseEntity.ok(CollectionModel.of(lista,
                linkTo(methodOn(InventarioController.class).porSucursal(sucursalId)).withSelfRel()));
    }

    @Operation(summary = "Ver stock de un producto en todas las sucursales")
    @ApiResponse(responseCode = "200", description = "Lista retornada")
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<CollectionModel<Inventario>> porProducto(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long productoId) {
        List<Inventario> lista = service.verStockPorProducto(productoId);
        if (lista.isEmpty()) return ResponseEntity.noContent().build();
        lista.forEach(this::agregarLinks);
        return ResponseEntity.ok(CollectionModel.of(lista,
                linkTo(methodOn(InventarioController.class).porProducto(productoId)).withSelfRel()));
    }

    @Operation(summary = "Ver stock exacto de un producto en una sucursal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventario encontrado"),
            @ApiResponse(responseCode = "404", description = "No hay inventario para ese producto/sucursal")
    })
    @GetMapping("/producto/{productoId}/sucursal/{sucursalId}")
    public ResponseEntity<?> stockExacto(
            @PathVariable Long productoId, @PathVariable Long sucursalId) {
        try {
            Inventario inv = service.verStock(productoId, sucursalId);
            agregarLinks(inv);
            return ResponseEntity.ok(inv);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ALERTAS
    // ─────────────────────────────────────────────────────────────
    @Operation(summary = "Ver alertas de stock bajo en todo el sistema")
    @ApiResponse(responseCode = "200", description = "Lista de productos con stock bajo")
    @GetMapping("/alertas")
    public ResponseEntity<CollectionModel<Inventario>> alertasGlobales() {
        List<Inventario> lista = service.obtenerAlertasGlobales();
        lista.forEach(this::agregarLinks);
        return ResponseEntity.ok(CollectionModel.of(lista,
                linkTo(methodOn(InventarioController.class).alertasGlobales()).withSelfRel()));
    }

    @Operation(summary = "Ver alertas de stock bajo en una sucursal específica")
    @ApiResponse(responseCode = "200", description = "Lista de alertas de la sucursal")
    @GetMapping("/alertas/sucursal/{sucursalId}")
    public ResponseEntity<CollectionModel<Inventario>> alertasPorSucursal(
            @PathVariable Long sucursalId) {
        List<Inventario> lista = service.obtenerAlertasPorSucursal(sucursalId);
        lista.forEach(this::agregarLinks);
        return ResponseEntity.ok(CollectionModel.of(lista,
                linkTo(methodOn(InventarioController.class).alertasPorSucursal(sucursalId)).withSelfRel()));
    }

    // ─────────────────────────────────────────────────────────────
    // MODIFICACIONES (solo ADMIN — validado en el service)
    // ─────────────────────────────────────────────────────────────
    @Operation(summary = "Aumentar stock de un producto en una sucursal (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock aumentado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @PostMapping("/aumentar")
    public ResponseEntity<?> aumentar(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "0") Long adminId,
            @RequestBody Map<String, Object> body) {
        try {
            Long productoId = getLong(body, "productoId");
            Long sucursalId = getLong(body, "sucursalId");
            Integer cantidad = getInt(body, "cantidad");
            String motivo = (String) body.getOrDefault("motivo", "Sin motivo");
            Inventario inv = service.aumentarStock(productoId, sucursalId, cantidad, motivo, adminId, rol);
            agregarLinks(inv);
            return ResponseEntity.ok(inv);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Reducir stock de un producto en una sucursal (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock reducido"),
            @ApiResponse(responseCode = "400", description = "Stock insuficiente o datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @PostMapping("/reducir")
    public ResponseEntity<?> reducir(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "0") Long adminId,
            @RequestBody Map<String, Object> body) {
        try {
            Long productoId = getLong(body, "productoId");
            Long sucursalId = getLong(body, "sucursalId");
            Integer cantidad = getInt(body, "cantidad");
            String motivo = (String) body.getOrDefault("motivo", "Sin motivo");
            Inventario inv = service.reducirStock(productoId, sucursalId, cantidad, motivo, adminId, rol);
            agregarLinks(inv);
            return ResponseEntity.ok(inv);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Ajustar stock a un valor exacto (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock ajustado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    })
    @PostMapping("/ajustar")
    public ResponseEntity<?> ajustar(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @RequestHeader(value = "X-Usuario-Id", defaultValue = "0") Long adminId,
            @RequestBody Map<String, Object> body) {
        try {
            Long productoId = getLong(body, "productoId");
            Long sucursalId = getLong(body, "sucursalId");
            Integer nuevaCantidad = getInt(body, "nuevaCantidad");
            String motivo = (String) body.getOrDefault("motivo", "Ajuste manual");
            Inventario inv = service.ajustarStock(productoId, sucursalId, nuevaCantidad, motivo, adminId, rol);
            agregarLinks(inv);
            return ResponseEntity.ok(inv);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar stock mínimo de alerta (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock mínimo actualizado"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN"),
            @ApiResponse(responseCode = "404", description = "Inventario no encontrado")
    })
    @PutMapping("/stock-minimo")
    public ResponseEntity<?> actualizarStockMinimo(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @RequestParam Long productoId,
            @RequestParam Long sucursalId,
            @RequestParam Integer stockMinimo) {
        try {
            Inventario inv = service.actualizarStockMinimo(productoId, sucursalId, stockMinimo, rol);
            agregarLinks(inv);
            return ResponseEntity.ok(inv);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HISTORIAL (solo ADMIN — validado en el service)
    // ─────────────────────────────────────────────────────────────
    @Operation(summary = "Ver historial completo de movimientos de una sucursal (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Historial retornado")
    @GetMapping("/historial/sucursal/{sucursalId}")
    public ResponseEntity<?> historialSucursal(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @PathVariable Long sucursalId) {
        try {
            return ResponseEntity.ok(service.historialPorSucursal(sucursalId, rol));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Ver historial de un producto en una sucursal (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Historial retornado")
    @GetMapping("/historial/producto/{productoId}/sucursal/{sucursalId}")
    public ResponseEntity<?> historialProducto(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @PathVariable Long productoId,
            @PathVariable Long sucursalId) {
        try {
            return ResponseEntity.ok(service.historialPorProductoYSucursal(productoId, sucursalId, rol));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Ver historial filtrado por tipo de movimiento (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Historial retornado")
    @GetMapping("/historial/sucursal/{sucursalId}/tipo/{tipo}")
    public ResponseEntity<?> historialPorTipo(
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol,
            @PathVariable Long sucursalId,
            @PathVariable TipoMovimiento tipo) {
        try {
            return ResponseEntity.ok(service.historialPorTipo(sucursalId, tipo, rol));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HATEOAS links
    // ─────────────────────────────────────────────────────────────
    private void agregarLinks(Inventario inv) {
        inv.add(linkTo(methodOn(InventarioController.class)
                .stockExacto(inv.getProductoId(), inv.getSucursalId())).withSelfRel());
        inv.add(linkTo(methodOn(InventarioController.class)
                .porSucursal(inv.getSucursalId())).withRel("sucursal"));
        inv.add(linkTo(methodOn(InventarioController.class)
                .porProducto(inv.getProductoId())).withRel("producto"));
        inv.add(linkTo(methodOn(InventarioController.class)
                .alertasGlobales()).withRel("alertas"));
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────
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