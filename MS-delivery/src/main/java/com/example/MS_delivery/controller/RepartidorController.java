package com.example.MS_delivery.controller;

import com.example.MS_delivery.dto.DeliveryDtos.RepartidorRequest;
import com.example.MS_delivery.dto.DeliveryDtos.UbicacionRequest;
import com.example.MS_delivery.model.Repartidor;
import com.example.MS_delivery.model.Repartidor.EstadoRepartidor;
import com.example.MS_delivery.service.RepartidorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/repartidores")
@RequiredArgsConstructor
public class RepartidorController {

    private final RepartidorService repartidorService;

    @GetMapping
    public ResponseEntity<List<Repartidor>> listar() {
        return ResponseEntity.ok(repartidorService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repartidorService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<Repartidor>> disponibles() {
        return ResponseEntity.ok(
                repartidorService.listarActivos().stream()
                        .filter(r -> r.getEstado() == EstadoRepartidor.LIBRE)
                        .toList()
        );
    }

    @GetMapping("/{id}/metricas")
    public ResponseEntity<?> metricas(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(repartidorService.obtenerMetricas(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody @Valid RepartidorRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(repartidorService.registrar(req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @RequestBody @Valid RepartidorRequest req) {
        try {
            return ResponseEntity.ok(repartidorService.actualizar(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            EstadoRepartidor estado = EstadoRepartidor.valueOf(
                    body.getOrDefault("estado", "").toUpperCase());
            repartidorService.cambiarEstado(id, estado);
            return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado a " + estado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Estado inválido. Valores: LIBRE, OCUPADO, INACTIVO"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/{id}/ubicacion")
    public ResponseEntity<?> actualizarUbicacion(@PathVariable Long id,
                                                  @RequestBody @Valid UbicacionRequest req) {
        try {
            repartidorService.actualizarUbicacion(id, req);
            return ResponseEntity.ok(Map.of("mensaje", "Ubicación actualizada"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        try {
            repartidorService.desactivar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Repartidor desactivado"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
