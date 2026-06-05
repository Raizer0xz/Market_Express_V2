package com.example.MS_productos.controller;

import com.example.MS_productos.model.Categoria;
import com.example.MS_productos.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(service.listarCategorias());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCategoria(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearCategoria(@RequestBody @Valid Categoria categoria) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.crearCategoria(categoria));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCategoria(@PathVariable Long id,
                                                 @RequestBody @Valid Categoria categoria) {
        try {
            return ResponseEntity.ok(service.actualizarCategoria(id, categoria));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCategoria(@PathVariable Long id) {
        try {
            service.eliminarCategoria(id);
            return ResponseEntity.ok(Map.of("mensaje", "Categoría eliminada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}