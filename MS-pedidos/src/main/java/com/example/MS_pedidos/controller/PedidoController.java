package com.example.MS_pedidos.controller;

import com.example.MS_pedidos.model.EstadoPedido;
import com.example.MS_pedidos.model.Pedido;
import com.example.MS_pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pedidos", description = "Gestión de pedidos de Market Express")
@SecurityRequirement(name = "bearerAuth")
public class PedidoController {

    private final PedidoService service;

    // -------------------------------------------------------------------------
    // GET /api/v1/pedidos
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar todos los pedidos",
            description = "Retorna todos los pedidos registrados en el sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada exitosamente"),
            @ApiResponse(responseCode = "204", description = "No hay pedidos registrados")
    })
    @GetMapping
    public ResponseEntity<CollectionModel<Pedido>> findAll() {
        log.info("Listando todos los pedidos");
        List<Pedido> pedidos = service.findAll();

        pedidos.forEach(this::agregarLinks);

        CollectionModel<Pedido> response = CollectionModel.of(
                pedidos,
                linkTo(methodOn(PedidoController.class).findAll()).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pedidos/usuario/{usuarioId}
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar pedidos por usuario",
            description = "Retorna todos los pedidos asociados a un usuario específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos del usuario",
                    content = @Content(schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "204", description = "El usuario no tiene pedidos")
    })
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<CollectionModel<Pedido>> findByUsuario(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long usuarioId) {
        log.info("Buscando pedidos del usuario: {}", usuarioId);
        List<Pedido> pedidos = service.findByUsuario(usuarioId);

        if (pedidos.isEmpty()) return ResponseEntity.noContent().build();

        pedidos.forEach(this::agregarLinks);

        CollectionModel<Pedido> response = CollectionModel.of(
                pedidos,
                linkTo(methodOn(PedidoController.class).findByUsuario(usuarioId)).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pedidos/sucursal/{sucursalId}?estado=PENDIENTE
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar pedidos por sucursal y estado",
            description = "Retorna pedidos filtrados por sucursal y estado. " +
                    "Estados posibles: PENDIENTE, EN_PROCESO, ENTREGADO, CANCELADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada"),
            @ApiResponse(responseCode = "204", description = "No hay pedidos con ese filtro")
    })
    @GetMapping("/sucursal/{sucursalId}")
    public ResponseEntity<CollectionModel<Pedido>> findBySucursalIdAndEstado(
            @Parameter(description = "ID de la sucursal", required = true, example = "1")
            @PathVariable Long sucursalId,
            @Parameter(description = "Estado del pedido", required = true, example = "PENDIENTE")
            @RequestParam EstadoPedido estado) {
        log.info("Buscando pedidos de sucursal {} con estado {}", sucursalId, estado);
        List<Pedido> pedidos = service.findBySucursalIdAndEstado(sucursalId, estado);

        if (pedidos.isEmpty()) return ResponseEntity.noContent().build();

        pedidos.forEach(this::agregarLinks);

        CollectionModel<Pedido> response = CollectionModel.of(
                pedidos,
                linkTo(methodOn(PedidoController.class).findBySucursalIdAndEstado(sucursalId, estado)).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pedidos
    // -------------------------------------------------------------------------
    @Operation(summary = "Crear nuevo pedido",
            description = "Registra un nuevo pedido en el sistema. " +
                    "El pedido queda en estado PENDIENTE hasta que sea procesado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente",
                    content = @Content(schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "400", description = "Datos del pedido inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\": \"El usuarioId es obligatorio\"}")))
    })
    @PostMapping
    public ResponseEntity<Pedido> save(@Valid @RequestBody Pedido pedido) {
        log.info("Creando pedido para usuario: {}, sucursal: {}", pedido.getUsuarioId(), pedido.getSucursalId());
        Pedido nuevo = service.save(pedido);
        log.info("Pedido creado con id: {}", nuevo.getId());

        agregarLinks(nuevo);

        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/pedidos/{id}/estado?nuevoEstado=EN_PROCESO
    // -------------------------------------------------------------------------
    @Operation(summary = "Cambiar estado del pedido",
            description = "Actualiza el estado de un pedido. " +
                    "Transiciones válidas: PENDIENTE → EN_PROCESO → ENTREGADO / CANCELADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado correctamente",
                    content = @Content(schema = @Schema(implementation = Pedido.class))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Pedido no encontrado con id: 1\"}")))
    })
    @PutMapping("/{id}/estado")
    public ResponseEntity<Pedido> update(
            @Parameter(description = "ID del pedido", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevo estado del pedido", required = true, example = "EN_PROCESO")
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

    // -------------------------------------------------------------------------
    // DELETE /api/v1/pedidos/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Eliminar pedido",
            description = "Elimina permanentemente un pedido por su ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido eliminado correctamente",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Pedido eliminado correctamente\"}"))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteById(
            @Parameter(description = "ID del pedido a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Eliminando pedido con id: {}", id);
        service.deleteById(id);
        log.info("Pedido {} eliminado", id);
        return ResponseEntity.ok(Map.of("mensaje", "Pedido eliminado correctamente"));
    }

    // -------------------------------------------------------------------------
    // HATEOAS links
    // -------------------------------------------------------------------------
    private void agregarLinks(Pedido pedido) {
        pedido.add(
                linkTo(methodOn(PedidoController.class).findAll()).withRel("todos")
        );
        pedido.add(
                linkTo(methodOn(PedidoController.class).update(pedido.getId(), null)).withRel("update")
        );
        pedido.add(
                linkTo(methodOn(PedidoController.class).deleteById(pedido.getId())).withRel("delete")
        );
    }
}