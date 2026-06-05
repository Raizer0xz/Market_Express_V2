package com.example.MS_sucursales.controller;

import com.example.MS_sucursales.model.Sucursal;
import com.example.MS_sucursales.service.SucursalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SucursalController — Endpoints de MS-sucursales (puerto 9091)
 *
 * GET    /sucursales              → listar todas (admin)
 * GET    /sucursales/abiertas     → listar solo abiertas (cliente/delivery)
 * GET    /sucursales/{id}         → buscar por ID
 * POST   /sucursales              → crear sucursal (admin)
 * PUT    /sucursales/{id}         → actualizar sucursal (admin)
 * PATCH  /sucursales/{id}/estado  → abrir/cerrar sucursal (admin)
 * DELETE /sucursales/{id}         → eliminar sucursal (admin)
 * GET    /sucursales/health       → verifica que el servicio está activo
 */
@RestController
@RequestMapping("/api/v1/sucursales")
@RequiredArgsConstructor
public class SucursalController {

    private final SucursalService sucursalService;

    // ─── GET /sucursales ──────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Sucursal>> listarTodas() {
        return ResponseEntity.ok(sucursalService.obtenerTodas());
    }

    // ─── GET /sucursales/abiertas ─────────────────────────────────────────────
    // Lo usan ms-productos y ms-delivery para saber qué sucursales están activas
    @GetMapping("/abiertas")
    public ResponseEntity<List<Sucursal>> listarAbiertas() {
        return ResponseEntity.ok(sucursalService.obtenerAbiertas());
    }

    // ─── GET /sucursales/{id} ─────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(sucursalService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── POST /sucursales ─────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody Sucursal sucursal) {
        try {
            Sucursal nueva = sucursalService.guardar(sucursal);
            return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── PUT /sucursales/{id} ─────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @Valid @RequestBody Sucursal datos) {
        try {
            Sucursal existente = sucursalService.obtenerPorId(id);
            existente.setNombre(datos.getNombre());
            existente.setDireccion(datos.getDireccion());
            existente.setLatitud(datos.getLatitud());
            existente.setLongitud(datos.getLongitud());
            existente.setHorarioApertura(datos.getHorarioApertura());
            existente.setHorarioCierre(datos.getHorarioCierre());
            // abierta y createdAt NO se tocan aquí
            return ResponseEntity.ok(sucursalService.guardar(existente));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── PATCH /sucursales/{id}/estado ────────────────────────────────────────
    // Solo cambia si la sucursal está abierta o cerrada, sin tocar el resto
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @RequestParam boolean abierta) {
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── DELETE /sucursales/{id} ──────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            sucursalService.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Sucursal eliminada correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─── GET /sucursales/health ───────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "servicio", "ms-sucursales",
                "estado", "activo",
                "puerto", "9091"
        ));
    }
}