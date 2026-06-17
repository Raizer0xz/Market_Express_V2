package com.example.MS_productos.controller;

import com.example.MS_productos.model.Categoria;
import com.example.MS_productos.service.CategoriaService;
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
 * CategoriaController — Endpoints de categorías (puerto 9092)
 *
 * GET    /api/v2/categorias       → listar todas
 * GET    /api/v2/categorias/{id}  → buscar por ID
 * POST   /api/v2/categorias       → crear
 * PUT    /api/v2/categorias/{id}  → actualizar
 * DELETE /api/v2/categorias/{id}  → eliminar
 */
@RestController
@RequestMapping("/api/v2/categorias")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Categorias", description = "Gestión de categorías de productos")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {

    private final CategoriaService service;

    // -------------------------------------------------------------------------
    // GET /api/v2/categorias
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar todas las categorías")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada exitosamente"),
            @ApiResponse(responseCode = "204", description = "No hay categorías registradas")
    })
    @GetMapping
    public ResponseEntity<CollectionModel<Categoria>> listarCategorias() {
        log.info("Listando todas las categorías");
        List<Categoria> categorias = service.listarCategorias();

        if (categorias.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        categorias.forEach(this::agregarLinks);

        CollectionModel<Categoria> response = CollectionModel.of(
                categorias,
                linkTo(methodOn(CategoriaController.class).listarCategorias()).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // GET /api/v2/categorias/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Buscar categoría por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría encontrada",
                    content = @Content(schema = @Schema(implementation = Categoria.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Categoría no encontrada: 1\"}")))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCategoria(
            @Parameter(description = "ID de la categoría", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Buscando categoría con id: {}", id);
        try {
            Categoria categoria = service.obtenerPorId(id);
            agregarLinks(categoria);
            return ResponseEntity.ok(categoria);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v2/categorias
    // -------------------------------------------------------------------------
    @Operation(summary = "Crear nueva categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoría creada",
                    content = @Content(schema = @Schema(implementation = Categoria.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<?> crearCategoria(@RequestBody @Valid Categoria categoria) {
        log.info("Creando categoría: {}", categoria.getNombre());
        try {
            Categoria nueva = service.crearCategoria(categoria);
            agregarLinks(nueva);
            return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v2/categorias/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Actualizar categoría")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría actualizada",
                    content = @Content(schema = @Schema(implementation = Categoria.class))),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCategoria(
            @Parameter(description = "ID de la categoría", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody @Valid Categoria categoria) {
        log.info("Actualizando categoría con id: {}", id);
        try {
            Categoria actualizada = service.actualizarCategoria(id, categoria);
            agregarLinks(actualizada);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v2/categorias/{id}
    // -------------------------------------------------------------------------
    @Operation(summary = "Eliminar categoría",
            description = "Elimina la categoría. Falla si tiene productos asociados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoría eliminada",
                    content = @Content(schema = @Schema(example = "{\"mensaje\": \"Categoría eliminada correctamente\"}"))),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar porque tiene productos asociados"),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCategoria(
            @Parameter(description = "ID de la categoría", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Eliminando categoría con id: {}", id);
        try {
            service.eliminarCategoria(id);
            return ResponseEntity.ok(Map.of("mensaje", "Categoría eliminada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // HATEOAS links
    // -------------------------------------------------------------------------
    private void agregarLinks(Categoria categoria) {
        categoria.add(
                linkTo(methodOn(CategoriaController.class).obtenerCategoria(categoria.getId())).withSelfRel()
        );
        categoria.add(
                linkTo(methodOn(CategoriaController.class).listarCategorias()).withRel("todas")
        );
        categoria.add(
                linkTo(methodOn(CategoriaController.class).actualizarCategoria(categoria.getId(), categoria)).withRel("update")
        );
        categoria.add(
                linkTo(methodOn(CategoriaController.class).eliminarCategoria(categoria.getId())).withRel("delete")
        );
        categoria.add(
                linkTo(methodOn(ProductoController.class).porCategoria(categoria.getId())).withRel("productos")
        );
    }
}