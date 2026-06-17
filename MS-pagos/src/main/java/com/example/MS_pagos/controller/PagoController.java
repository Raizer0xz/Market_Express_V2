package com.example.MS_pagos.controller;

import com.example.MS_pagos.modelo.MetodoPago;
import com.example.MS_pagos.modelo.Pago;
import com.example.MS_pagos.modelo.PagoRequest;
import com.example.MS_pagos.service.PagoService;
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
 * PagoController - Endpoints de MS-pagos (puerto 9093)
 *
 * POST /api/v1/pagos/procesar          → crear pago (CLIENTE o ADMIN)
 * GET  /api/v1/pagos/metodos           → listar metodos disponibles
 * GET  /api/v1/pagos/pedido/{pedidoId} → pagos por pedido
 * POST /api/v1/pagos/confirmar         → confirmar transaccion (solo ADMIN)
 * GET  /api/v1/pagos/health            → health check
 */
@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos", description = "Gestión de pagos de pedidos en Market Express")
@SecurityRequirement(name = "bearerAuth")
public class PagoController {

    private final PagoService pagoService;

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/procesar
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Procesar un pago",
            description = "Crea y procesa un pago para un pedido. Solo pueden acceder CLIENTE o ADMIN. " +
                    "El rol se obtiene del header X-Usuario-Rol enviado por el Gateway."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago procesado exitosamente",
                    content = @Content(schema = @Schema(implementation = Pago.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o método de pago no reconocido",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Metodo de pago invalido: 'EFECTIVO'\"}"))),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Solo clientes o administradores pueden crear pagos\"}")))
    })
    @PostMapping("/procesar")
    public ResponseEntity<?> crearPago(
            @Valid @RequestBody PagoRequest request,
            @Parameter(description = "Rol del usuario autenticado (viene del Gateway)", example = "CLIENTE")
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol) {

        log.info("Solicitud de pago recibida. Pedido: {} | Rol: {}", request.getPedidoId(), rol);

        if (!rol.equals("CLIENTE") && !rol.equals("ADMIN")) {
            log.warn("Acceso denegado a /pagos/procesar. Rol: {}", rol);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo clientes o administradores pueden crear pagos"));
        }

        try {
            Pago pago = pagoService.procesarPago(request);
            agregarLinks(pago);
            return ResponseEntity.status(HttpStatus.CREATED).body(pago);
        } catch (IllegalArgumentException e) {
            log.warn("Metodo de pago invalido: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/metodos
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Listar métodos de pago disponibles",
            description = "Retorna los métodos de pago aceptados: TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA_BANCARIA, PAYPAL."
    )
    @ApiResponse(responseCode = "200", description = "Lista de métodos retornada")
    @GetMapping("/metodos")
    public ResponseEntity<List<MetodoPago>> getMetodos() {
        log.info("Consultando metodos de pago disponibles");
        return ResponseEntity.ok(pagoService.obtenerMetodosDisponibles());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/pedido/{pedidoId}
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Listar pagos de un pedido",
            description = "Retorna todos los pagos asociados a un pedido específico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de pagos retornada"),
            @ApiResponse(responseCode = "204", description = "No hay pagos para ese pedido")
    })
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<CollectionModel<Pago>> porPedido(
            @Parameter(description = "ID del pedido", required = true, example = "1")
            @PathVariable Long pedidoId) {
        log.info("Consultando pagos del pedido: {}", pedidoId);
        List<Pago> pagos = pagoService.obtenerPorPedido(pedidoId);

        if (pagos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        pagos.forEach(this::agregarLinks);

        CollectionModel<Pago> response = CollectionModel.of(
                pagos,
                linkTo(methodOn(PagoController.class).porPedido(pedidoId)).withSelfRel()
        );

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/pagos/confirmar
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Confirmar transacción de pago",
            description = "Actualiza el estado de un pago a COMPLETADO o RECHAZADO. Solo ADMIN puede ejecutar este endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción confirmada",
                    content = @Content(schema = @Schema(implementation = Pago.class))),
            @ApiResponse(responseCode = "403", description = "Solo administradores pueden confirmar pagos",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Solo administradores pueden confirmar pagos\"}"))),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Pago no encontrado con transaccionId: abc-123\"}")))
    })
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmar(
            @Parameter(description = "ID de la transacción a confirmar", required = true, example = "abc-123-uuid")
            @RequestParam String transaccionId,
            @Parameter(description = "Estado de la transacción: SUCCESS o FAILED", required = true, example = "SUCCESS")
            @RequestParam String status,
            @Parameter(description = "Rol del usuario autenticado (viene del Gateway)", example = "ADMIN")
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol) {

        log.info("Solicitud de confirmacion. TransaccionId: {} | Status: {} | Rol: {}", transaccionId, status, rol);

        if (!rol.equals("ADMIN")) {
            log.warn("Acceso denegado a /pagos/confirmar. Rol: {}", rol);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Solo administradores pueden confirmar pagos"));
        }

        try {
            Pago pago = pagoService.confirmarTransaccion(transaccionId, status);
            agregarLinks(pago);
            return ResponseEntity.ok(pago);
        } catch (RuntimeException e) {
            log.warn("Transaccion no encontrada: {}", transaccionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/health
    // -------------------------------------------------------------------------
    @Operation(summary = "Health check", description = "Verifica que el microservicio de pagos está activo.")
    @ApiResponse(responseCode = "200", description = "Servicio activo")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "servicio", "ms-pagos",
                "estado",   "activo",
                "puerto",   "9093"
        ));
    }

    // -------------------------------------------------------------------------
    // HATEOAS links
    // -------------------------------------------------------------------------
    private void agregarLinks(Pago pago) {
        pago.add(
                linkTo(methodOn(PagoController.class).porPedido(pago.getPedidoId())).withRel("pagos-del-pedido")
        );
        pago.add(
                linkTo(methodOn(PagoController.class).getMetodos()).withRel("metodos-disponibles")
        );
        pago.add(
                linkTo(methodOn(PagoController.class)
                        .confirmar(pago.getTransaccionId(), "SUCCESS", "ADMIN")).withRel("confirmar")
        );
    }
}