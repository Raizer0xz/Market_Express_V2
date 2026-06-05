package com.example.MS_pedidos.controller;

import com.example.MS_pedidos.model.EstadoPedido;
import com.example.MS_pedidos.model.Pedido;
import com.example.MS_pedidos.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PedidoController - Endpoints de MS-pedidos (puerto 9094)
 *
 * GET    /api/v1/pedidos                          -> listar todos
 * GET    /api/v1/pedidos/usuario/{usuarioId}      -> pedidos por usuario
 * GET    /api/v1/pedidos/sucursal/{sucursalId}?estado=X -> pedidos por sucursal y estado
 * POST   /api/v1/pedidos                          -> crear pedido
 * PUT    /api/v1/pedidos/{id}/estado?nuevoEstado=X-> cambiar estado
 * DELETE /api/v1/pedidos/{id}                     -> eliminar pedido
 */
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Slf4j
public class PedidoController {

    private final PedidoService service;

    // --- GET /api/v1/pedidos ---
    @GetMapping
    public ResponseEntity<List<Pedido>> findAll() {
        log.info("Listando todos los pedidos");
        return ResponseEntity.ok(service.findAll());
    }

    // --- GET /api/v1/pedidos/usuario/{usuarioId} ---
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Pedido>> findByUsuario(@PathVariable Long usuarioId) {
        log.info("Buscando pedidos del usuario: {}", usuarioId);
        List<Pedido> pedidos = service.findByUsuario(usuarioId);
        if (pedidos.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 - lista vacia
        }
        return ResponseEntity.ok(pedidos);
    }

    // --- GET /api/v1/pedidos/sucursal/{sucursalId}?estado=PENDIENTE ---
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<List<Pedido>> findBySucursalIdAndEstado(
            @PathVariable Long sucursalId,
            @RequestParam EstadoPedido estado) {
        log.info("Buscando pedidos de sucursal {} con estado {}", sucursalId, estado);
        List<Pedido> pedidos = service.findBySucursalIdAndEstado(sucursalId, estado);
        if (pedidos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(pedidos);
    }

    // --- POST /api/v1/pedidos ---
    // FIX: @Valid agregado para activar las validaciones del modelo Pedido
    @PostMapping
    public ResponseEntity<Pedido> save(@Valid @RequestBody Pedido pedido) {
        log.info("Creando pedido para usuario: {}, sucursal: {}", pedido.getUsuarioId(), pedido.getSucursalId());
        Pedido nuevo = service.save(pedido);
        log.info("Pedido creado con id: {}", nuevo.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // --- PUT /api/v1/pedidos/{id}/estado?nuevoEstado=EN_PROCESO ---
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestParam EstadoPedido nuevoEstado) {
        log.info("Cambiando estado del pedido {} a {}", id, nuevoEstado);
        return service.update(id, nuevoEstado)
                .map(p -> {
                    log.info("Estado del pedido {} actualizado a {}", id, nuevoEstado);
                    return ResponseEntity.ok(p);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null));
    }

    // --- DELETE /api/v1/pedidos/{id} ---
    // FIX: tenia el metodo sin @DeleteMapping - era un metodo huerfano que nunca se llamaba por HTTP
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteById(@PathVariable Long id) {
        log.info("Eliminando pedido con id: {}", id);
        service.deleteById(id);
        log.info("Pedido {} eliminado", id);
        return ResponseEntity.ok(Map.of("mensaje", "Pedido eliminado correctamente"));
    }
}