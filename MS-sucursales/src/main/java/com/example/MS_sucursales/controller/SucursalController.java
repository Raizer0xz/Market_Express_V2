package com.example.MS_sucursales.controller;

import com.example.MS_sucursales.model.Sucursal;
import com.example.MS_sucursales.service.SucursalService;
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
 * SucursalController — Endpoints de MS-sucursales (puerto 9091)
 *
 * GET    /api/v1/sucursales              → listar todas
 * GET    /api/v1/sucursales/abiertas     → listar solo abiertas
 * GET    /api/v1/sucursales/{id}         → buscar por ID
 * POST   /api/v1/sucursales              → crear sucursal
 * PUT    /api/v1/sucursales/{id}         → actualizar sucursal
 * PATCH  /api/v1/sucursales/{id}/estado  → abrir/cerrar sucursal
 * DELETE /api/v1/sucursales/{id}         → eliminar sucursal
 * GET    /api/v1/sucursales/health       → health check
 */
@RestController
@RequestMapping("/api/v1/sucursales")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sucursales", description = "Gestión de sucursales de Market Express")
@SecurityRequirement(name = "bearerAuth")
public class SucursalController {

    private final SucursalService sucursalService;

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar todas las sucursales",
            description = "Retorna todas las sucursales registradas (abiertas y cerradas).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada exitosamente"),
            @ApiResponse(responseCode = "204", description = "No hay sucursales registradas")
    })
    @GetMapping
    public ResponseEntity<CollectionModel<Sucursal>> listarTodas() {
        log.info("Listando todas las sucursales");
        List<Sucursal> sucursales = sucursalService.obtenerTodas();

        if (sucursales.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        sucursales.forEach(this::agregarLinks);

        CollectionModel<Sucursal> response = CollectionModel.of(
                sucursales,
                linkTo(methodOn(SucursalController.class).listarTodas()).withSelfRel(),
                linkTo(methodOn(SucursalController.class).listarAbiertas()).withRel("abiertas")
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales/abiertas
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar sucursales abiertas",
            description = "Usado por MS-productos y MS-delivery para saber qué sucursales están activas.")
    @ApiResponse(responseCode = "200", description = "Lista de sucursales abiertas")
    @GetMapping("/abiertas")
    public ResponseEntity<CollectionModel<Sucursal>> listarAbiertas() {
        log.info("Listando sucursales abiertas");
        List<Sucursal> sucursales = sucursalService.obtenerAbiertas();

        sucursales.forEach(this::agregarLinks);

        CollectionModel<Sucursal> response = CollectionModel.of(
                sucursales,
                linkTo(methodOn(SucursalController.class).listarAbiertas()).withSelfRel(),
                linkTo(methodOn(SucursalController.class).listarTodas()).withRel("todas")
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Buscar sucursal por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal encontrada",
                    content = @Content(schema = @Schema(implementation = Sucursal.class))),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Sucursal no encontrada con el ID: 1\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(
            @Parameter(description = "ID de la sucursal", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Buscando sucursal con id: {}", id);
        try {
            Sucursal sucursal = sucursalService.obtenerPorId(id);
            agregarLinks(sucursal);
            return ResponseEntity.ok(sucursal);
        } catch (RuntimeException e) {
            log.warn("Sucursal no encontrada con id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/sucursales
    // -------------------------------------------------------------------------
    @Operation(summary = "Crear nueva sucursal",
            description = "Registra una nueva sucursal. Por defecto queda como abierta.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sucursal creada exitosamente",
                    content = @Content(schema = @Schema(implementation = Sucursal.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody Sucursal sucursal) {
        log.info("Creando nueva sucursal: {}", sucursal.getNombre());
        try {
            Sucursal nueva = sucursalService.guardar(sucursal);
            agregarLinks(nueva);
            log.info("Sucursal creada con id: {}", nueva.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
        } catch (Exception e) {
            log.error("Error al crear sucursal: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/sucursales/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Actualizar sucursal",
            description = "Actualiza nombre, dirección, coordenadas y horarios. El campo 'abierta' NO se toca aquí, usar PATCH /estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal actualizada",
                    content = @Content(schema = @Schema(implementation = Sucursal.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @Parameter(description = "ID de la sucursal a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody Sucursal datos) {
        log.info("Actualizando sucursal con id: {}", id);
        try {
            Sucursal existente = sucursalService.obtenerPorId(id);
            existente.setNombre(datos.getNombre());
            existente.setDireccion(datos.getDireccion());
            existente.setLatitud(datos.getLatitud());
            existente.setLongitud(datos.getLongitud());
            existente.setHorarioApertura(datos.getHorarioApertura());
            existente.setHorarioCierre(datos.getHorarioCierre());
            // abierta y createdAt NO se tocan aquí
            Sucursal actualizada = sucursalService.guardar(existente);
            agregarLinks(actualizada);
            log.info("Sucursal {} actualizada correctamente", id);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            log.warn("No se pudo actualizar sucursal con id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/sucursales/{id}/estado
    // -------------------------------------------------------------------------
    @Operation(summary = "Abrir o cerrar sucursal",
            description = "Cambia solo el campo 'abierta'. Usar ?abierta=true para abrir, ?abierta=false para cerrar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Sucursal abierta correctamente\", \"id\": 1, \"abierta\": true}"))),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    })
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @Parameter(description = "ID de la sucursal", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "true para abrir, false para cerrar", required = true, example = "true")
            @RequestParam boolean abierta) {
        log.info("Cambiando estado de sucursal {} a abierta={}", id, abierta);
        try {
            Sucursal sucursal = sucursalService.obtenerPorId(id);
            sucursal.setAbierta(abierta);
            sucursalService.guardar(sucursal);
            String estado = abierta ? "abierta" : "cerrada";
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Sucursal " + estado + " correctamente",
                    "id", id,
                    "abierta", abierta
            ));
        } catch (RuntimeException e) {
            log.warn("Sucursal no encontrada para cambio de estado, id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/sucursales/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Eliminar sucursal",
            description = "Elimina permanentemente la sucursal con el ID indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sucursal eliminada",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Sucursal eliminada correctamente\"}"))),
            @ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(
            @Parameter(description = "ID de la sucursal a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Eliminando sucursal con id: {}", id);
        try {
            sucursalService.eliminar(id);
            log.info("Sucursal {} eliminada correctamente", id);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal eliminada correctamente"));
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar sucursal con id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/sucursales/health
    // -------------------------------------------------------------------------
    @Operation(summary = "Health check", description = "Verifica que el microservicio esté activo.")
    @ApiResponse(responseCode = "200", description = "Servicio activo")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "servicio", "ms-sucursales",
                "estado",   "activo",
                "puerto",   "9091"
        ));
    }

    // -------------------------------------------------------------------------
    // HATEOAS: agrega links hipermedia a cada sucursal
    // -------------------------------------------------------------------------
    private void agregarLinks(Sucursal sucursal) {
        sucursal.add(
                linkTo(methodOn(SucursalController.class).buscarPorId(sucursal.getId()))
                        .withSelfRel()
        );
        sucursal.add(
                linkTo(methodOn(SucursalController.class).listarTodas())
                        .withRel("todas")
        );
        sucursal.add(
                linkTo(methodOn(SucursalController.class).actualizar(sucursal.getId(), sucursal))
                        .withRel("update")
        );
        sucursal.add(
                linkTo(methodOn(SucursalController.class).eliminar(sucursal.getId()))
                        .withRel("delete")
        );
        sucursal.add(
                linkTo(methodOn(SucursalController.class).cambiarEstado(sucursal.getId(), !sucursal.isAbierta()))
                        .withRel("cambiar-estado")
        );
    }
}