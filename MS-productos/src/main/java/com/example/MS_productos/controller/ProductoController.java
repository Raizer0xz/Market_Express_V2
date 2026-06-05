package com.example.MS_productos.controller;

import com.example.MS_productos.model.Producto;
import com.example.MS_productos.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ProductoController — Endpoints de productos (puerto 9092)
 *
 * GET    /api/v2/productos                    → listar activos
 * GET    /api/v2/productos/{id}               → buscar por ID
 * GET    /api/v2/productos/categoria/{catId}  → listar por categoría
 * GET    /api/v2/productos/buscar?nombre=X    → buscar por nombre
 * POST   /api/v2/productos                    → crear
 * PUT    /api/v2/productos/{id}               → actualizar
 * DELETE /api/v2/productos/{id}               → desactivar (baja lógica)
 */
@RestController
@RequestMapping("/api/v2/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(service.listarProductos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerProducto(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/categoria/{catId}")
    public ResponseEntity<?> porCategoria(@PathVariable Long catId) {
        try {
            return ResponseEntity.ok(service.listarPorCategoria(catId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Producto>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(service.buscarPorNombre(nombre));
    }

    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody @Valid Producto producto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(service.crearProducto(producto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(@PathVariable Long id,
                                                @RequestBody @Valid Producto producto) {
        try {
            return ResponseEntity.ok(service.actualizarProducto(id, producto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> desactivarProducto(@PathVariable Long id) {
        try {
            service.desactivarProducto(id);
            return ResponseEntity.ok(Map.of("mensaje", "Producto desactivado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}