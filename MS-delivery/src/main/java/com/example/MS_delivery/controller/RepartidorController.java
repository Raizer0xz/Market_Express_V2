package com.example.MS_delivery.controller;

import com.example.MS_delivery.dto.DeliveryDtos.MetricasRepartidorResponse;
import com.example.MS_delivery.dto.DeliveryDtos.RepartidorRequest;
import com.example.MS_delivery.dto.DeliveryDtos.UbicacionRequest;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.Repartidor.EstadoRepartidor;
import com.example.MS_delivery.service.RepartidorService;
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

/**
 * RepartidorController — Endpoints de repartidores (puerto 9090)
 *
 * GET    /api/v1/repartidores                      → listar activos
 * GET    /api/v1/repartidores/{id}                 → buscar por ID
 * POST   /api/v1/repartidores                      → registrar
 * PUT    /api/v1/repartidores/{id}                 → actualizar
 * DELETE /api/v1/repartidores/{id}                 → desactivar
 * PATCH  /api/v1/repartidores/{id}/estado          → cambiar estado
 * POST   /api/v1/repartidores/{id}/ubicacion       → actualizar GPS
 * GET    /api/v1/repartidores/{id}/metricas        → métricas del repartidor
 */
@RestController
@RequestMapping("/api/v1/repartidores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Repartidores", description = "Gestión de repartidores y actualización de ubicación GPS")
@SecurityRequirement(name = "bearerAuth")
public class RepartidorController {

    private final RepartidorService repartidorService;

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar repartidores activos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada"),
            @ApiResponse(responseCode = "204", description = "No hay repartidores activos")
    })
    @GetMapping
    public ResponseEntity<CollectionModel<Repartidor>> listarActivos() {
        log.info("Listando repartidores activos");
        List<Repartidor> lista = repartidorService.listarActivos();

        if (lista.isEmpty()) return ResponseEntity.noContent().build();

        lista.forEach(this::agregarLinks);

        return ResponseEntity.ok(CollectionModel.of(lista,
                linkTo(methodOn(RepartidorController.class).listarActivos()).withSelfRel()));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Buscar repartidor por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repartidor encontrado",
                    content = @Content(schema = @Schema(implementation = Repartidor.class))),
            @ApiResponse(responseCode = "404", description = "Repartidor no encontrado",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Repartidor no encontrado: 1\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(
            @Parameter(description = "ID del repartidor", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Buscando repartidor con id: {}", id);
        try {
            Repartidor r = repartidorService.obtenerPorId(id);
            agregarLinks(r);
            return ResponseEntity.ok(r);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/repartidores
    // -------------------------------------------------------------------------
    @Operation(summary = "Registrar nuevo repartidor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Repartidor registrado",
                    content = @Content(schema = @Schema(implementation = Repartidor.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un repartidor con ese email")
    })
    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody RepartidorRequest request) {
        log.info("Registrando repartidor: {}", request.getEmail());
        try {
            Repartidor nuevo = repartidorService.registrar(request);
            agregarLinks(nuevo);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/repartidores/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Actualizar datos del repartidor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repartidor actualizado",
                    content = @Content(schema = @Schema(implementation = Repartidor.class))),
            @ApiResponse(responseCode = "404", description = "Repartidor no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @Parameter(description = "ID del repartidor", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody RepartidorRequest request) {
        log.info("Actualizando repartidor con id: {}", id);
        try {
            Repartidor actualizado = repartidorService.actualizar(id, request);
            agregarLinks(actualizado);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/repartidores/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Desactivar repartidor",
            description = "Baja lógica: el repartidor queda activo=false, no se elimina de la BD.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Repartidor desactivado",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Repartidor desactivado correctamente\"}"))),
            @ApiResponse(responseCode = "404", description = "Repartidor no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> desactivar(
            @Parameter(description = "ID del repartidor", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Desactivando repartidor con id: {}", id);
        try {
            repartidorService.desactivar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Repartidor desactivado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/repartidores/{id}/estado
    // -------------------------------------------------------------------------
    @Operation(summary = "Cambiar estado del repartidor",
            description = "Estados: LIBRE, OCUPADO, INACTIVO")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Estado actualizado a LIBRE\"}"))),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "404", description = "Repartidor no encontrado")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @Parameter(description = "ID del repartidor", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevo estado: LIBRE, OCUPADO, INACTIVO", required = true, example = "LIBRE")
            @RequestParam String estado) {
        log.info("Cambiando estado de repartidor {} a {}", id, estado);
        try {
            EstadoRepartidor nuevoEstado = EstadoRepartidor.valueOf(estado.toUpperCase());
            repartidorService.cambiarEstado(id, nuevoEstado);
            return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado a " + nuevoEstado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado inválido: " + estado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/repartidores/{id}/ubicacion
    // -------------------------------------------------------------------------
    @Operation(summary = "Actualizar ubicación GPS del repartidor",
            description = "Llamado desde la app del repartidor en cada ping GPS. Actualiza posición actual y guarda en historial.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ubicación actualizada",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Ubicación actualizada\"}"))),
            @ApiResponse(responseCode = "404", description = "Repartidor no encontrado")
    })
    @PostMapping("/{id}/ubicacion")
    public ResponseEntity<?> actualizarUbicacion(
            @Parameter(description = "ID del repartidor", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UbicacionRequest request) {
        log.info("Actualizando ubicacion de repartidor {}: lat={} lng={}", id, request.getLatitud(), request.getLongitud());
        try {
            repartidorService.actualizarUbicacion(id, request);
            return ResponseEntity.ok(Map.of("mensaje", "Ubicación actualizada"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/repartidores/{id}/metricas
    // -------------------------------------------------------------------------
    @Operation(summary = "Obtener métricas del repartidor",
            description = "Retorna total de entregas, exitosas, fallidas y tasa de éxito.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Métricas retornadas"),
            @ApiResponse(responseCode = "404", description = "Repartidor no encontrado")
    })
    @GetMapping("/{id}/metricas")
    public ResponseEntity<?> metricas(
            @Parameter(description = "ID del repartidor", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Obteniendo metricas del repartidor: {}", id);
        try {
            MetricasRepartidorResponse metricas = repartidorService.obtenerMetricas(id);
            return ResponseEntity.ok(metricas);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // HATEOAS links
    // -------------------------------------------------------------------------
    private void agregarLinks(Repartidor repartidor) {
        repartidor.add(
                linkTo(methodOn(RepartidorController.class).obtener(repartidor.getId())).withSelfRel()
        );
        repartidor.add(
                linkTo(methodOn(RepartidorController.class).listarActivos()).withRel("todos")
        );
        repartidor.add(
                linkTo(methodOn(RepartidorController.class).actualizar(repartidor.getId(), null)).withRel("update")
        );
        repartidor.add(
                linkTo(methodOn(RepartidorController.class).desactivar(repartidor.getId())).withRel("desactivar")
        );
        repartidor.add(
                linkTo(methodOn(RepartidorController.class).metricas(repartidor.getId())).withRel("metricas")
        );
        repartidor.add(
                linkTo(methodOn(DeliveryController.class).historial(repartidor.getId())).withRel("historial-deliveries")
        );
    }
}