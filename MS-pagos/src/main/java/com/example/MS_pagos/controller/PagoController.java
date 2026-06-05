package com.example.MS_pagos.controller;

import com.example.MS_pagos.modelo.MetodoPago;
import com.example.MS_pagos.modelo.Pago;
import com.example.MS_pagos.modelo.PagoRequest;
import com.example.MS_pagos.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PagoController - Endpoints de MS-pagos (puerto 9093)
 *
 * POST /api/v1/pagos/procesar          -> crear pago (CLIENTE o ADMIN)
 * GET  /api/v1/pagos/metodos           -> listar metodos disponibles
 * GET  /api/v1/pagos/pedido/{pedidoId} -> pagos por pedido
 * POST /api/v1/pagos/confirmar         -> confirmar transaccion (ADMIN)
 *
 * El rol llega como header X-Usuario-Rol, inyectado por el JwtAuthFilter del Gateway.
 */
@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
@Slf4j
public class PagoController {

    private final PagoService pagoService;

    // POST /api/v1/pagos/procesar - solo CLIENTE o ADMIN
    // FIX: @Valid agregado para activar validaciones de PagoRequest
    @PostMapping("/procesar")
    public ResponseEntity<?> crearPago(
            @Valid @RequestBody PagoRequest request,
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol) {

        log.info("Solicitud de pago recibida. Pedido: {} | Rol: {}", request.getPedidoId(), rol);

        if (!rol.equals("CLIENTE") && !rol.equals("ADMIN")) {
            log.warn("Acceso denegado a /pagos/procesar. Rol: {}", rol);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo clientes o administradores pueden crear pagos"));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.procesarPago(request));
    }

    // GET /api/v1/pagos/metodos - publico dentro de la red
    @GetMapping("/metodos")
    public ResponseEntity<List<MetodoPago>> getMetodos() {
        return ResponseEntity.ok(pagoService.obtenerMetodosDisponibles());
    }

    // GET /api/v1/pagos/pedido/{pedidoId}
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<List<Pago>> porPedido(@PathVariable Long pedidoId) {
        log.info("Consultando pagos del pedido: {}", pedidoId);
        return ResponseEntity.ok(pagoService.obtenerPorPedido(pedidoId));
    }

    // POST /api/v1/pagos/confirmar - solo ADMIN
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmar(
            @RequestParam String transaccionId,
            @RequestParam String status,
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol) {

        log.info("Solicitud de confirmacion de transaccion: {} | Status: {} | Rol: {}",
                transaccionId, status, rol);

        if (!rol.equals("ADMIN")) {
            log.warn("Acceso denegado a /pagos/confirmar. Rol: {}", rol);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo administradores pueden confirmar pagos"));
        }

        return ResponseEntity.ok(pagoService.confirmarTransaccion(transaccionId, status));
    }
}