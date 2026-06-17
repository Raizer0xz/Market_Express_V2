package com.example.MS_productos.controller;

import com.example.MS_productos.model.PrecioProducto;
import com.example.MS_productos.service.PrecioProductoService;
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
 * PrecioProductoController — Endpoints de precios (puerto 9092)
 *
 * GET    /api/v2/precios/producto/{productoId}                    → precios de un producto
 * GET    /api/v2/precios/producto/{productoId}/sucursal/{sucId}   → precio por producto y sucursal
 * POST   /api/v2/precios                                          → registrar precio
 * DELETE /api/v2/precios/{id}                                     → eliminar precio
 */
@RestController
@RequestMapping("/api/v2/precios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Precios", description = "Gestión de precios de productos por sucursal")
@SecurityRequirement(name = "bearerAuth")
public class PrecioProductoController {

    private final PrecioProductoService service;

    // -------------------------------------------------------------------------
    // GET /api/v2/precios/producto/{productoId}
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar precios de un producto",
            description = "Retorna todos los precios registrados para un producto en todas las sucursales.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de precios retornada"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Producto no encontrado: 1\"}")))
    })
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<?> porProducto(
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long productoId) {
        log.info("Listando precios del producto: {}", productoId);
        try {
            List<PrecioProducto> precios = service.listarPorProducto(productoId);
            precios.forEach(p -> agregarLinks(p));

            CollectionModel<PrecioProducto> response = CollectionModel.of(
                    precios,
                    linkTo(methodOn(PrecioProductoController.class).porProducto(productoId)).withSelfRel()
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/precios/producto/{productoId}/sucursal/{sucursalId}
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar precios de un producto en una sucursal específica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de precios retornada"),
            @ApiResponse(responseCode = "404", description = "Producto o sucursal no encontrado")
    })
    @GetMapping("/producto/{productoId}/sucursal/{sucursalId}")
    public ResponseEntity<?> porProductoYSucursal(
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long productoId,
            @Parameter(description = "ID de la sucursal", required = true, example = "1")
            @PathVariable Long sucursalId) {
        log.info("Listando precios del producto {} en sucursal {}", productoId, sucursalId);
        try {
            List<PrecioProducto> precios = service.listarPorProductoYSucursal(productoId, sucursalId);
            precios.forEach(p -> agregarLinks(p));

            CollectionModel<PrecioProducto> response = CollectionModel.of(
                    precios,
                    linkTo(methodOn(PrecioProductoController.class)
                            .porProductoYSucursal(productoId, sucursalId)).withSelfRel(),
                    linkTo(methodOn(PrecioProductoController.class)
                            .porProducto(productoId)).withRel("todos-los-precios")
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/precios
    // -------------------------------------------------------------------------
    @Operation(summary = "Registrar precio para un producto",
            description = "Asocia un precio a un producto en una sucursal específica. " +
                    "Enviar {\"producto\": {\"id\": 1}, \"sucursalId\": 2, \"precio\": 1500.00}")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Precio registrado",
                    content = @Content(schema = @Schema(implementation = PrecioProducto.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o producto no encontrado")
    })
    @PostMapping
    public ResponseEntity<?> crearPrecio(@RequestBody @Valid PrecioProducto precio) {
        log.info("Registrando precio para producto: {}", precio.getProducto().getId());
        try {
            PrecioProducto nuevo = service.crearPrecio(precio);
            agregarLinks(nuevo);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/precios/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Eliminar precio",
            description = "Elimina permanentemente un precio por su ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Precio eliminado",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Precio eliminado correctamente\"}"))),
            @ApiResponse(responseCode = "404", description = "Precio no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarPrecio(
            @Parameter(description = "ID del precio", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Eliminando precio con id: {}", id);
        try {
            service.eliminarPrecio(id);
            return ResponseEntity.ok(Map.of("mensaje", "Precio eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // HATEOAS links
    // -------------------------------------------------------------------------
    private void agregarLinks(PrecioProducto precio) {
        precio.add(
                linkTo(methodOn(PrecioProductoController.class)
                        .porProducto(precio.getProducto().getId())).withSelfRel()
        );
        precio.add(
                linkTo(methodOn(PrecioProductoController.class)
                        .eliminarPrecio(precio.getId())).withRel("delete")
        );
        precio.add(
                linkTo(methodOn(ProductoController.class)
                        .obtenerProducto(precio.getProducto().getId())).withRel("producto")
        );
    }
}