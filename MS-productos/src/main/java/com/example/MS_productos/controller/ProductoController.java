package com.example.MS_productos.controller;

import com.example.MS_productos.model.Producto;
import com.example.MS_productos.service.ProductoService;
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
 * ProductoController — Endpoints de productos (puerto 9092)
 *
 * GET    /api/v2/productos                   → listar activos
 * GET    /api/v2/productos/{id}              → buscar por ID
 * GET    /api/v2/productos/categoria/{catId} → listar por categoría
 * GET    /api/v2/productos/buscar?nombre=X   → buscar por nombre
 * POST   /api/v2/productos                   → crear
 * PUT    /api/v2/productos/{id}              → actualizar
 * DELETE /api/v2/productos/{id}             → desactivar (baja lógica)
 */
@RestController
@RequestMapping("/api/v2/productos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Productos", description = "Gestión de productos del catálogo Market Express")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {

    private final ProductoService service;

    // -------------------------------------------------------------------------
    // GET /api/v2/productos
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar productos activos",
            description = "Retorna todos los productos con activo=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada exitosamente"),
            @ApiResponse(responseCode = "204", description = "No hay productos activos")
    })
    @GetMapping
    public ResponseEntity<CollectionModel<Producto>> listarProductos() {
        log.info("Listando todos los productos activos");
        List<Producto> productos = service.listarProductos();

        if (productos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        productos.forEach(this::agregarLinks);

        CollectionModel<Producto> response = CollectionModel.of(
                productos,
                linkTo(methodOn(ProductoController.class).listarProductos()).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/productos/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Buscar producto por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado",
                    content = @Content(schema = @Schema(implementation = Producto.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Producto no encontrado: 1\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerProducto(
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Buscando producto con id: {}", id);
        try {
            Producto producto = service.obtenerPorId(id);
            agregarLinks(producto);
            return ResponseEntity.ok(producto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/productos/categoria/{catId}
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar productos por categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de productos de la categoría"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @GetMapping("/categoria/{catId}")
    public ResponseEntity<?> porCategoria(
            @Parameter(description = "ID de la categoría", required = true, example = "1")
            @PathVariable Long catId) {
        log.info("Listando productos de categoría: {}", catId);
        try {
            List<Producto> productos = service.listarPorCategoria(catId);
            productos.forEach(this::agregarLinks);

            CollectionModel<Producto> response = CollectionModel.of(
                    productos,
                    linkTo(methodOn(ProductoController.class).porCategoria(catId)).withSelfRel(),
                    linkTo(methodOn(ProductoController.class).listarProductos()).withRel("todos")
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/productos/buscar?nombre=X
    // -------------------------------------------------------------------------
    @Operation(summary = "Buscar productos por nombre",
            description = "Búsqueda parcial e insensible a mayúsculas sobre el nombre del producto.")
    @ApiResponse(responseCode = "200", description = "Lista de productos que coinciden con el nombre")
    @GetMapping("/buscar")
    public ResponseEntity<CollectionModel<Producto>> buscarPorNombre(
            @Parameter(description = "Nombre o parte del nombre a buscar", required = true, example = "leche")
            @RequestParam String nombre) {
        log.info("Buscando productos con nombre: {}", nombre);
        List<Producto> productos = service.buscarPorNombre(nombre);
        productos.forEach(this::agregarLinks);

        CollectionModel<Producto> response = CollectionModel.of(
                productos,
                linkTo(methodOn(ProductoController.class).buscarPorNombre(nombre)).withSelfRel(),
                linkTo(methodOn(ProductoController.class).listarProductos()).withRel("todos")
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/productos
    // -------------------------------------------------------------------------
    @Operation(summary = "Crear nuevo producto",
            description = "La categoría debe existir previamente. Enviar {\"categoria\": {\"id\": 1}} en el body.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado",
                    content = @Content(schema = @Schema(implementation = Producto.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o categoría no existe")
    })
    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody @Valid Producto producto) {
        log.info("Creando producto: {}", producto.getNombre());
        try {
            Producto nuevo = service.crearProducto(producto);
            agregarLinks(nuevo);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v2/productos/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Actualizar producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado",
                    content = @Content(schema = @Schema(implementation = Producto.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody @Valid Producto producto) {
        log.info("Actualizando producto con id: {}", id);
        try {
            Producto actualizado = service.actualizarProducto(id, producto);
            agregarLinks(actualizado);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/productos/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Desactivar producto",
            description = "Baja lógica: el producto queda con activo=false, no se elimina de la BD.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto desactivado",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Producto desactivado correctamente\"}"))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> desactivarProducto(
            @Parameter(description = "ID del producto", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Desactivando producto con id: {}", id);
        try {
            service.desactivarProducto(id);
            return ResponseEntity.ok(Map.of("mensaje", "Producto desactivado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // HATEOAS links
    // -------------------------------------------------------------------------
    private void agregarLinks(Producto producto) {
        producto.add(
                linkTo(methodOn(ProductoController.class).obtenerProducto(producto.getId())).withSelfRel()
        );
        producto.add(
                linkTo(methodOn(ProductoController.class).listarProductos()).withRel("todos")
        );
        producto.add(
                linkTo(methodOn(ProductoController.class).actualizarProducto(producto.getId(), producto)).withRel("update")
        );
        producto.add(
                linkTo(methodOn(ProductoController.class).desactivarProducto(producto.getId())).withRel("desactivar")
        );
        if (producto.getCategoria() != null) {
            producto.add(
                    linkTo(methodOn(ProductoController.class).porCategoria(producto.getCategoria().getId())).withRel("categoria")
            );
            producto.add(
                    linkTo(methodOn(PrecioProductoController.class).porProducto(producto.getId())).withRel("precios")
            );
        }
    }
}