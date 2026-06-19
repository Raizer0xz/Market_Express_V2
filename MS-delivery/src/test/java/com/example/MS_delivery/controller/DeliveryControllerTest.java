package com.example.MS_delivery.controller;

import com.example.MS_delivery.dto.DeliveryDtos.IniciarDeliveryRequest;
import com.example.MS_delivery.dto.DeliveryDtos.NotaRequest;
import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Delivery.EstadoDelivery;
import com.example.MS_delivery.model.UbicacionHistorial;
import com.example.MS_delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery", description = "Gestión de entregas: asignación, ciclo de vida y seguimiento GPS")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryControllerTest {

    private final DeliveryService deliveryService;

    // ── Asignación ───────────────────────────────────────────────────────────

    @Operation(summary = "Asignar automáticamente un delivery al repartidor más cercano")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Delivery creado y asignado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o sin repartidores disponibles")
    })
    @PostMapping("/asignar")
    public ResponseEntity<?> asignar(@RequestBody @Valid IniciarDeliveryRequest req) {
        try {
            Delivery delivery = deliveryService.asignarAutomaticamente(req);
            agregarLinks(delivery);
            return ResponseEntity.status(HttpStatus.CREATED).body(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    @Operation(summary = "Obtener un delivery por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery encontrado"),
            @ApiResponse(responseCode = "404", description = "Delivery no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@Parameter(description = "ID del delivery") @PathVariable Long id) {
        try {
            Delivery delivery = deliveryService.obtenerPorId(id);
            agregarLinks(delivery);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Obtener el delivery activo de un pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery encontrado"),
            @ApiResponse(responseCode = "404", description = "No hay delivery para ese pedido")
    })
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<?> obtenerPorPedido(@PathVariable Long pedidoId) {
        try {
            Delivery delivery = deliveryService.obtenerPorPedidoId(pedidoId);
            agregarLinks(delivery);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Listar deliveries filtrados por estado",
            description = "Estados: PENDIENTE, EN_RUTA, ENTREGADO, FALLIDO, CANCELADO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "400", description = "Estado inválido")
    })
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> porEstado(@PathVariable String estado) {
        try {
            EstadoDelivery e = EstadoDelivery.valueOf(estado.toUpperCase());
            List<Delivery> lista = deliveryService.listarPorEstado(e);
            lista.forEach(this::agregarLinks);
            return ResponseEntity.ok(CollectionModel.of(lista,
                    linkTo(methodOn(DeliveryControllerTest.class).porEstado(estado)).withSelfRel()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Estado inválido: " + estado));
        }
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Operation(summary = "Iniciar la ruta de un delivery (PENDIENTE → EN_RUTA)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ruta iniciada"),
            @ApiResponse(responseCode = "400", description = "Estado actual no permite iniciar ruta")
    })
    @PutMapping("/{id}/iniciar-ruta")
    public ResponseEntity<?> iniciarRuta(@PathVariable Long id) {
        try {
            Delivery delivery = deliveryService.iniciarRuta(id);
            agregarLinks(delivery);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Confirmar entrega de un delivery (EN_RUTA → ENTREGADO)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entrega confirmada"),
            @ApiResponse(responseCode = "400", description = "Estado actual no permite confirmar entrega")
    })
    @PutMapping("/{id}/entregar")
    public ResponseEntity<?> confirmarEntrega(@PathVariable Long id) {
        try {
            Delivery delivery = deliveryService.confirmarEntrega(id);
            agregarLinks(delivery);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Reportar fallo en la entrega de un delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fallo registrado"),
            @ApiResponse(responseCode = "400", description = "Estado actual no permite reportar fallo")
    })
    @PutMapping("/{id}/fallo")
    public ResponseEntity<?> reportarFallo(@PathVariable Long id,
                                           @RequestBody(required = false) NotaRequest req) {
        try {
            String notas = req != null ? req.getNotas() : null;
            Delivery delivery = deliveryService.reportarFallo(id, notas);
            agregarLinks(delivery);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Cancelar un delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery cancelado"),
            @ApiResponse(responseCode = "400", description = "No se puede cancelar (ya entregado)")
    })
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                      @RequestBody(required = false) NotaRequest req) {
        try {
            String notas = req != null ? req.getNotas() : null;
            Delivery delivery = deliveryService.cancelar(id, notas);
            agregarLinks(delivery);
            return ResponseEntity.ok(delivery);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Seguimiento GPS (para el cliente) ────────────────────────────────────

    @Operation(summary = "Obtener ubicación en tiempo real del repartidor asignado a un pedido",
            description = "Devuelve posición del repartidor + estado del delivery + ETA estimado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ubicación retornada"),
            @ApiResponse(responseCode = "404", description = "No hay delivery activo para el pedido")
    })
    @GetMapping("/pedido/{pedidoId}/ubicacion")
    public ResponseEntity<?> ubicacionPorPedido(@PathVariable Long pedidoId) {
        try {
            return ResponseEntity.ok(deliveryService.obtenerUbicacionPorPedido(pedidoId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Obtener la ruta GPS completa recorrida en un delivery")
    @ApiResponse(responseCode = "200", description = "Ruta retornada (puntos ordenados por timestamp ASC)")
    @GetMapping("/{id}/ruta")
    public ResponseEntity<List<UbicacionHistorial>> ruta(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.obtenerRutaDelivery(id));
    }

    // ── Historial ────────────────────────────────────────────────────────────

    @Operation(summary = "Obtener el historial de deliveries de un repartidor")
    @ApiResponse(responseCode = "200", description = "Historial retornado")
    @GetMapping("/repartidor/{repId}/historial")
    public ResponseEntity<List<Delivery>> historial(@PathVariable Long repId) {
        List<Delivery> lista = deliveryService.historialRepartidor(repId);
        lista.forEach(this::agregarLinks);
        return ResponseEntity.ok(lista);
    }

    // ── HATEOAS links ────────────────────────────────────────────────────────
    private void agregarLinks(Delivery delivery) {
        delivery.add(linkTo(methodOn(DeliveryControllerTest.class).obtener(delivery.getId())).withSelfRel());
        delivery.add(linkTo(methodOn(DeliveryControllerTest.class).ruta(delivery.getId())).withRel("ruta"));
        delivery.add(linkTo(methodOn(DeliveryControllerTest.class)
                .obtenerPorPedido(delivery.getPedidoId())).withRel("pedido"));
        if (delivery.getRepartidor() != null) {
            delivery.add(linkTo(methodOn(DeliveryControllerTest.class)
                    .historial(delivery.getRepartidor().getId())).withRel("historial-repartidor"));
        }
    }
}