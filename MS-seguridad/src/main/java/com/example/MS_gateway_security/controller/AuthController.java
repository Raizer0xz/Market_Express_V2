package com.example.MS_gateway_security.controller;

import com.example.MS_gateway_security.dto.*;
import com.example.MS_gateway_security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — Endpoints del MS-seguridad (puerto 8082)
 *
 * POST /auth/registrar  → crea credenciales para un usuario nuevo
 * POST /auth/login      → autentica y devuelve JWT
 * POST /auth/validar    → valida token (lo usan los demás microservicios)
 * GET  /auth/health     → verifica que el servicio está activo
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticacion", description = "Endpoints de registro, login y validacion de tokens JWT")
public class AuthController {

    private final AuthService authService;

    // -------------------------------------------------------------------------
    // POST /auth/registrar
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Registrar credenciales",
            description = "Crea las credenciales (email + password) para un usuario ya existente en MS-USUARIOS. " +
                    "El usuarioId debe existir previamente en MS-usuarios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Credenciales creadas y JWT generado",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (validación fallida)",
                    content = @Content(schema = @Schema(example = "{\"error\": \"El email es obligatorio\"}"))),
            @ApiResponse(responseCode = "409", description = "Ya existe cuenta con ese email o usuarioId",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Ya existe una cuenta con ese email\"}")))
    })
    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegisterRequest request) {
        log.info("Solicitud de registro para email: {}", request.getEmail());
        try {
            AuthResponse response = authService.registrar(request);
            log.info("Credenciales creadas para usuarioId: {}", request.getUsuarioId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.warn("Conflicto al registrar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/login
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Iniciar sesion",
            description = "Autentica con email y password. Retorna un JWT Bearer token con duracion de 24h."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso, JWT retornado",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(example = "{\"error\": \"El email es obligatorio\"}"))),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o cuenta desactivada",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Email o contrasena incorrectos\"}")))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Solicitud de login para email: {}", request.getEmail());
        try {
            AuthResponse response = authService.login(request);
            log.info("Login exitoso para email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Login fallido para email: {} — {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/validar
    // -------------------------------------------------------------------------
    @Operation(
            summary = "Validar token JWT",
            description = "Verifica si un token JWT es válido y no ha expirado. " +
                    "Lo usan internamente MS-gateway y el resto de microservicios. " +
                    "Enviar el token en el header: Authorization: Bearer <token>"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token válido — retorna usuarioId, email y rol",
                    content = @Content(schema = @Schema(implementation = ValidarTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Header Authorization ausente o con formato incorrecto",
                    content = @Content(schema = @Schema(example = "{\"valido\": false, \"mensaje\": \"Header invalido. Formato esperado: Bearer <token>\"}"))),
            @ApiResponse(responseCode = "401", description = "Token expirado o inválido",
                    content = @Content(schema = @Schema(example = "{\"valido\": false, \"mensaje\": \"Token expirado o invalido\"}")))
    })
    @PostMapping("/validar")
    public ResponseEntity<ValidarTokenResponse> validarToken(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Solicitud de validacion de token");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Header Authorization invalido o ausente");
            return ResponseEntity.badRequest().body(
                    ValidarTokenResponse.builder()
                            .valido(false)
                            .mensaje("Header invalido. Formato esperado: Bearer <token>")
                            .build());
        }

        String token = authHeader.substring(7);
        ValidarTokenResponse response = authService.validarToken(token);

        if (response.isValido()) {
            log.info("Token valido para usuarioId: {}", response.getUsuarioId());
            return ResponseEntity.ok(response);
        } else {
            log.warn("Token invalido: {}", response.getMensaje());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // -------------------------------------------------------------------------
    // GET /auth/health
    // -------------------------------------------------------------------------
    @Operation(summary = "Health check", description = "Verifica que el microservicio de seguridad está activo.")
    @ApiResponse(responseCode = "200", description = "Servicio activo",
            content = @Content(schema = @Schema(example = "{\"servicio\": \"ms-seguridad\", \"estado\": \"activo\", \"puerto\": \"8082\"}")))
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "servicio", "ms-seguridad",
                "estado",   "activo",
                "puerto",   "8082"
        ));
    }
}