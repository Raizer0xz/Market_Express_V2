package com.example.MS_carrito.controller;

import com.example.MS_carrito.dto.ItemCarritoDetalleDTO;
import com.example.MS_carrito.model.Carrito;
import com.example.MS_carrito.model.ItemCarrito;
import com.example.MS_carrito.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v2/carritos")
public class CarritoController {

    @Autowired
    private CarritoService service;

    @PostMapping
    public ResponseEntity<Carrito> crearCarrito(@RequestBody @Valid Carrito carrito) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCarrito(carrito));
    }

    @GetMapping("/activo/{usuarioId}")
    public ResponseEntity<Carrito> obtenerActivo(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(service.obtenerCarritoActivo(usuarioId));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<ItemCarrito>> listarItems(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarItems(id));
    }

    // ✅ ENDPOINT NUEVO — ítems con nombre del producto
    @GetMapping("/{id}/items/detalle")
    public ResponseEntity<List<ItemCarritoDetalleDTO>> listarItemsConDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarItemsConDetalle(id));
    }

    @GetMapping("/{id}/total")
    public ResponseEntity<BigDecimal> calcularTotal(@PathVariable Long id) {
        return ResponseEntity.ok(service.calcularTotal(id));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ItemCarrito> agregarItem(
            @PathVariable Long id,
            @RequestBody @Valid ItemCarrito item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarItem(id, item));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> eliminarItem(@PathVariable Long itemId) {
        service.eliminarItem(itemId);
        return ResponseEntity.noContent().build();
    }

    // ✅ ENDPOINT NUEVO — eliminar carrito completo
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCarrito(@PathVariable Long id) {
        service.eliminarCarrito(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<Carrito> confirmarCarrito(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirmarCarrito(id));
    }
}