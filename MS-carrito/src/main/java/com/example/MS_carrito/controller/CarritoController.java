package com.example.MS_carrito.controller;

import com.example.MS_carrito.dto.ItemCarritoDetalleDTO;
import com.example.MS_carrito.model.Carrito;
import com.example.MS_carrito.model.ItemCarrito;
import com.example.MS_carrito.service.CarritoService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * CarritoController — Endpoints de MS-carrito (puerto 8086)
 *
 * POST   /api/v2/carritos                     → crear carrito
 * GET    /api/v2/carritos/activo/{usuarioId}  → obtener carrito activo
 * GET    /api/v2/carritos/{id}/items          → listar ítems
 * GET    /api/v2/carritos/{id}/items/detalle  → listar ítems con nombre producto
 * GET    /api/v2/carritos/{id}/total          → calcular total
 * POST   /api/v2/carritos/{id}/items          → agregar ítem
 * DELETE /api/v2/carritos/items/{itemId}      → eliminar ítem
 * DELETE /api/v2/carritos/{id}               → eliminar carrito
 * PUT    /api/v2/carritos/{id}/confirmar      → confirmar carrito
 */
@RestController
@RequestMapping("/api/v2/carritos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Carritos", description = "Gestión del carrito de compras de Market Express")
@SecurityRequirement(name = "bearerAuth")
public class CarritoController {

    private final CarritoService service;

    // -------------------------------------------------------------------------
    // POST /api/v2/carritos
    // -------------------------------------------------------------------------
    @Operation(summary = "Crear carrito",
            description = "Crea un carrito ACTIVO para el usuario. Falla si ya tiene uno activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Carrito creado",
                    content = @Content(schema = @Schema(implementation = Carrito.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "El usuario ya tiene un carrito activo",
                    content = @Content(schema = @Schema(example = "{\"error\": \"El usuario ya tiene un carrito activo\"}")))
    })
    @PostMapping
    public ResponseEntity<?> crearCarrito(@RequestBody @Valid Carrito carrito) {
        log.info("Creando carrito para usuario: {}", carrito.getUsuarioId());
        try {
            Carrito nuevo = service.crearCarrito(carrito);
            agregarLinks(nuevo);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/activo/{usuarioId}
    // -------------------------------------------------------------------------
    @Operation(summary = "Obtener carrito activo del usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito encontrado",
                    content = @Content(schema = @Schema(implementation = Carrito.class))),
            @ApiResponse(responseCode = "404", description = "No hay carrito activo para ese usuario",
                    content = @Content(schema = @Schema(example = "{\"error\": \"No hay carrito activo para el usuario: 1\"}")))
    })
    @GetMapping("/activo/{usuarioId}")
    public ResponseEntity<?> obtenerActivo(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long usuarioId) {
        log.info("Buscando carrito activo del usuario: {}", usuarioId);
        try {
            Carrito carrito = service.obtenerCarritoActivo(usuarioId);
            agregarLinks(carrito);
            return ResponseEntity.ok(carrito);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/{id}/items
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar ítems del carrito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de ítems retornada"),
            @ApiResponse(responseCode = "204", description = "El carrito está vacío")
    })
    @GetMapping("/{id}/items")
    public ResponseEntity<List<ItemCarrito>> listarItems(
            @Parameter(description = "ID del carrito", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Listando ítems del carrito: {}", id);
        List<ItemCarrito> items = service.listarItems(id);
        if (items.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(items);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/{id}/items/detalle
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar ítems con detalle del producto",
            description = "Igual que /items pero incluye nombre y unidad de medida obtenidos desde MS-PRODUCTOS via Feign.")
    @ApiResponse(responseCode = "200", description = "Lista de ítems con detalle retornada")
    @GetMapping("/{id}/items/detalle")
    public ResponseEntity<List<ItemCarritoDetalleDTO>> listarItemsConDetalle(
            @Parameter(description = "ID del carrito", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Listando ítems con detalle del carrito: {}", id);
        return ResponseEntity.ok(service.listarItemsConDetalle(id));
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/carritos/{id}/total
    // -------------------------------------------------------------------------
    @Operation(summary = "Calcular total del carrito",
            description = "Suma precioUnitario * cantidad de todos los ítems.")
    @ApiResponse(responseCode = "200", description = "Total calculado",
            content = @Content(schema = @Schema(example = "15750.00")))
    @GetMapping("/{id}/total")
    public ResponseEntity<BigDecimal> calcularTotal(
            @Parameter(description = "ID del carrito", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Calculando total del carrito: {}", id);
        return ResponseEntity.ok(service.calcularTotal(id));
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/carritos/{id}/items
    // -------------------------------------------------------------------------
    @Operation(summary = "Agregar ítem al carrito",
            description = "Si el producto ya existe en el carrito, suma la cantidad en vez de duplicar.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ítem agregado",
                    content = @Content(schema = @Schema(implementation = ItemCarrito.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    @PostMapping("/{id}/items")
    public ResponseEntity<?> agregarItem(
            @Parameter(description = "ID del carrito", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody @Valid ItemCarrito item) {
        log.info("Agregando ítem al carrito {}: productoId={}", id, item.getProductoId());
        try {
            ItemCarrito nuevo = service.agregarItem(id, item);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/carritos/items/{itemId}
    // -------------------------------------------------------------------------
    @Operation(summary = "Eliminar ítem del carrito")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ítem eliminado",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Ítem eliminado correctamente\"}"))),
            @ApiResponse(responseCode = "404", description = "Ítem no encontrado")
    })
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> eliminarItem(
            @Parameter(description = "ID del ítem", required = true, example = "1")
            @PathVariable Long itemId) {
        log.info("Eliminando ítem: {}", itemId);
        try {
            service.eliminarItem(itemId);
            return ResponseEntity.ok(Map.of("mensaje", "Ítem eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/carritos/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Eliminar carrito completo",
            description = "Elimina el carrito y todos sus ítems (cascade).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito eliminado",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Carrito eliminado correctamente\"}"))),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCarrito(
            @Parameter(description = "ID del carrito", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Eliminando carrito: {}", id);
        try {
            service.eliminarCarrito(id);
            return ResponseEntity.ok(Map.of("mensaje", "Carrito eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v2/carritos/{id}/confirmar
    // -------------------------------------------------------------------------
    @Operation(summary = "Confirmar carrito",
            description = "Cambia el estado del carrito a CONFIRMADO. Después de esto ya no se pueden agregar ítems.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carrito confirmado",
                    content = @Content(schema = @Schema(implementation = Carrito.class))),
            @ApiResponse(responseCode = "404", description = "Carrito no encontrado")
    })
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmarCarrito(
            @Parameter(description = "ID del carrito", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Confirmando carrito: {}", id);
        try {
            Carrito confirmado = service.confirmarCarrito(id);
            agregarLinks(confirmado);
            return ResponseEntity.ok(confirmado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // HATEOAS links
    // -------------------------------------------------------------------------
    private void agregarLinks(Carrito carrito) {
        carrito.add(
                linkTo(methodOn(CarritoController.class).obtenerActivo(carrito.getUsuarioId())).withSelfRel()
        );
        carrito.add(
                linkTo(methodOn(CarritoController.class).listarItems(carrito.getId())).withRel("items")
        );
        carrito.add(
                linkTo(methodOn(CarritoController.class).listarItemsConDetalle(carrito.getId())).withRel("items-detalle")
        );
        carrito.add(
                linkTo(methodOn(CarritoController.class).calcularTotal(carrito.getId())).withRel("total")
        );
        carrito.add(
                linkTo(methodOn(CarritoController.class).confirmarCarrito(carrito.getId())).withRel("confirmar")
        );
        carrito.add(
                linkTo(methodOn(CarritoController.class).eliminarCarrito(carrito.getId())).withRel("delete")
        );
    }
}