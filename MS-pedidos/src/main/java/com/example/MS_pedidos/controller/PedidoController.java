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

// HATEOAS paso 2: importar estas dos líneas
import org.springframework.hateoas.CollectionModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

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
    public ResponseEntity<CollectionModel<Pedido>> findAll() {
        log.info("Listando todos los pedidos");
        List<Pedido> pedidos = service.findAll();

        // HATEOAS: a cada pedido de la lista le agregamos sus links individuales
        pedidos.forEach(this::agregarLinks);

        // HATEOAS: CollectionModel envuelve la lista y le agrega un link "self" a la colección
        CollectionModel<Pedido> response = CollectionModel.of(
                pedidos,
                linkTo(methodOn(PedidoController.class).findAll()).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // --- GET /api/v1/pedidos/usuario/{usuarioId} ---
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<CollectionModel<Pedido>> findByUsuario(@PathVariable Long usuarioId) {
        log.info("Buscando pedidos del usuario: {}", usuarioId);
        List<Pedido> pedidos = service.findByUsuario(usuarioId);
        if (pedidos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        pedidos.forEach(this::agregarLinks);

        CollectionModel<Pedido> response = CollectionModel.of(
                pedidos,
                linkTo(methodOn(PedidoController.class).findByUsuario(usuarioId)).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // --- GET /api/v1/pedidos/sucursal/{sucursalId}?estado=PENDIENTE ---
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<CollectionModel<Pedido>> findBySucursalIdAndEstado(
            @PathVariable Long sucursalId,
            @RequestParam EstadoPedido estado) {
        log.info("Buscando pedidos de sucursal {} con estado {}", sucursalId, estado);
        List<Pedido> pedidos = service.findBySucursalIdAndEstado(sucursalId, estado);
        if (pedidos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        pedidos.forEach(this::agregarLinks);

        CollectionModel<Pedido> response = CollectionModel.of(
                pedidos,
                linkTo(methodOn(PedidoController.class).findBySucursalIdAndEstado(sucursalId, estado)).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // --- POST /api/v1/pedidos ---
    @PostMapping
    public ResponseEntity<Pedido> save(@Valid @RequestBody Pedido pedido) {
        log.info("Creando pedido para usuario: {}, sucursal: {}", pedido.getUsuarioId(), pedido.getSucursalId());
        Pedido nuevo = service.save(pedido);
        log.info("Pedido creado con id: {}", nuevo.getId());

        // HATEOAS: al pedido recién creado le agregamos sus links
        agregarLinks(nuevo);

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // --- PUT /api/v1/pedidos/{id}/estado?nuevoEstado=EN_PROCESO ---
    @PutMapping("/{id}/estado")
    public ResponseEntity<Pedido> update(@PathVariable Long id,
                                         @RequestParam EstadoPedido nuevoEstado) {
        log.info("Cambiando estado del pedido {} a {}", id, nuevoEstado);
        return service.update(id, nuevoEstado)
                .map(p -> {
                    log.info("Estado del pedido {} actualizado a {}", id, nuevoEstado);
                    agregarLinks(p);
                    return ResponseEntity.ok(p);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    // --- DELETE /api/v1/pedidos/{id} ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteById(@PathVariable Long id) {
        log.info("Eliminando pedido con id: {}", id);
        service.deleteById(id);
        log.info("Pedido {} eliminado", id);
        return ResponseEntity.ok(Map.of("mensaje", "Pedido eliminado correctamente"));
    }

    // -----------------------------------------------------------------------
    // HATEOAS paso 2: método privado que agrega los links a un Pedido
    //
    // linkTo(methodOn(PedidoController.class).findAll())
    //   -> genera la URL del método findAll() de este controller = /api/v1/pedidos
    //
    // .withRel("todos")
    //   -> le pone el nombre "todos" al link en el JSON
    //
    // .withSelfRel()
    //   -> es un shortcut para .withRel("self"), el link canónico del propio recurso
    // -----------------------------------------------------------------------
    private void agregarLinks(Pedido pedido) {
        pedido.add(
                linkTo(methodOn(PedidoController.class).findAll())
                        .withRel("todos")
        );
        pedido.add(
                linkTo(methodOn(PedidoController.class).update(pedido.getId(), null))
                        .withRel("update")
        );
        pedido.add(
                linkTo(methodOn(PedidoController.class).deleteById(pedido.getId()))
                        .withRel("delete")
        );
    }
}