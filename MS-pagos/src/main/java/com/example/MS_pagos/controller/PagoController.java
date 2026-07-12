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
    @Operation(summary = "Procesar un pago",
            description = "Crea y procesa un pago para un pedido. Solo pueden acceder CLIENTE o ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago procesado exitosamente",
                    content = @Content(schema = @Schema(implementation = Pago.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o método de pago no reconocido"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Solo clientes o administradores pueden crear pagos\"}")))
    })
    @PostMapping("/procesar")
    public ResponseEntity<?> crearPago(
            @Valid @RequestBody PagoRequest request,
            @Parameter(description = "Rol del usuario autenticado", example = "CLIENTE")
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol) {
        log.info("Solicitud de pago recibida. Pedido: {} | Rol: {}", request.getPedidoId(), rol);
        try {
            Pago pago = pagoService.procesarPago(request, rol);
            agregarLinks(pago);
            return ResponseEntity.status(HttpStatus.CREATED).body(pago);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/metodos
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar métodos de pago disponibles")
    @ApiResponse(responseCode = "200", description = "Lista de métodos retornada")
    @GetMapping("/metodos")
    public ResponseEntity<List<MetodoPago>> getMetodos() {
        return ResponseEntity.ok(pagoService.obtenerMetodosDisponibles());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/pedido/{pedidoId}
    // -------------------------------------------------------------------------
    @Operation(summary = "Listar pagos de un pedido")
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

        if (pagos.isEmpty()) return ResponseEntity.noContent().build();

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
    @Operation(summary = "Confirmar transacción de pago",
            description = "Solo ADMIN puede ejecutar este endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transacción confirmada"),
            @ApiResponse(responseCode = "403", description = "Solo administradores pueden confirmar pagos"),
            @ApiResponse(responseCode = "404", description = "Transacción no encontrada")
    })
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmar(
            @Parameter(description = "ID de la transacción", required = true, example = "abc-123-uuid")
            @RequestParam String transaccionId,
            @Parameter(description = "SUCCESS o FAILED", required = true, example = "SUCCESS")
            @RequestParam String status,
            @Parameter(description = "Rol del usuario autenticado", example = "ADMIN")
            @RequestHeader(value = "X-Usuario-Rol", defaultValue = "") String rol) {
        log.info("Confirmando transaccion: {} | Rol: {}", transaccionId, rol);
        try {
            Pago pago = pagoService.confirmarTransaccion(transaccionId, status, rol);
            agregarLinks(pago);
            return ResponseEntity.ok(pago);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/pagos/health
    // -------------------------------------------------------------------------
    @Operation(summary = "Health check")
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
        pago.add(linkTo(methodOn(PagoController.class).porPedido(pago.getPedidoId()))
                .withRel("pagos-del-pedido"));
        pago.add(linkTo(methodOn(PagoController.class).getMetodos())
                .withRel("metodos-disponibles"));
        pago.add(linkTo(methodOn(PagoController.class).confirmar(pago.getTransaccionId(), "SUCCESS", "ADMIN"))
                .withRel("confirmar"));
    }
}