package com.example.MS_delivery.controller;

import com.example.MS_delivery.dto.DeliveryDtos.IniciarDeliveryRequest;
import com.example.MS_delivery.dto.DeliveryDtos.NotaRequest;
import com.example.MS_delivery.model.Delivery;
import com.example.MS_delivery.model.Delivery.EstadoDelivery;
import com.example.MS_delivery.model.UbicacionHistorial;
import com.example.MS_delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ── Asignación ───────────────────────────────────────────────────────────

    @PostMapping("/asignar")
    public ResponseEntity<?> asignar(@RequestBody @Valid IniciarDeliveryRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(deliveryService.asignarAutomaticamente(req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(deliveryService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<?> obtenerPorPedido(@PathVariable Long pedidoId) {
        try {
            return ResponseEntity.ok(deliveryService.obtenerPorPedidoId(pedidoId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> porEstado(@PathVariable String estado) {
        try {
            EstadoDelivery e = EstadoDelivery.valueOf(estado.toUpperCase());
            return ResponseEntity.ok(deliveryService.listarPorEstado(e));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Estado inválido: " + estado));
        }
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @PutMapping("/{id}/iniciar-ruta")
    public ResponseEntity<?> iniciarRuta(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(deliveryService.iniciarRuta(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/entregar")
    public ResponseEntity<?> confirmarEntrega(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(deliveryService.confirmarEntrega(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/fallo")
    public ResponseEntity<?> reportarFallo(@PathVariable Long id,
                                           @RequestBody(required = false) NotaRequest req) {
        try {
            String notas = req != null ? req.getNotas() : null;
            return ResponseEntity.ok(deliveryService.reportarFallo(id, notas));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                      @RequestBody(required = false) NotaRequest req) {
        try {
            String notas = req != null ? req.getNotas() : null;
            return ResponseEntity.ok(deliveryService.cancelar(id, notas));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Seguimiento GPS (para el cliente) ────────────────────────────────────

    /**
     * El cliente llama este endpoint cada ~10 seg para ver dónde está su pedido.
     * Devuelve: posición del repartidor + estado del delivery + ETA estimado.
     *
     * Ejemplo respuesta:
     * {
     *   "repartidorId": 3,
     *   "nombreRepartidor": "Carlos Pérez",
     *   "latitud": -33.4489,
     *   "longitud": -70.6693,
     *   "ultimaActualizacion": "2026-05-13T18:30:00",
     *   "estadoDelivery": "EN_RUTA",
     *   "etaMinutos": 8
     * }
     */
    @GetMapping("/pedido/{pedidoId}/ubicacion")
    public ResponseEntity<?> ubicacionPorPedido(@PathVariable Long pedidoId) {
        try {
            return ResponseEntity.ok(deliveryService.obtenerUbicacionPorPedido(pedidoId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Ruta GPS completa recorrida durante un delivery.
     * Lista de puntos ordenados por timestamp ASC.
     */
    @GetMapping("/{id}/ruta")
    public ResponseEntity<List<UbicacionHistorial>> ruta(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryService.obtenerRutaDelivery(id));
    }

    // ── Historial ────────────────────────────────────────────────────────────

    @GetMapping("/repartidor/{repId}/historial")
    public ResponseEntity<List<Delivery>> historial(@PathVariable Long repId) {
        return ResponseEntity.ok(deliveryService.historialRepartidor(repId));
    }
}
