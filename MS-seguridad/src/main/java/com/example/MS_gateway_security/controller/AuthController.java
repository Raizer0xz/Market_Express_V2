package com.example.MS_gateway_security.controller;

import com.example.MS_gateway_security.dto.*;
import com.example.MS_gateway_security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — Endpoints del ms-seguridad (puerto 8082)
 *
 * POST /auth/registrar  -> crea credenciales para un usuario nuevo
 * POST /auth/login      -> autentica y devuelve JWT
 * POST /auth/validar    -> valida token (lo usan los demas microservicios)
 * GET  /auth/health     -> verifica que el servicio esta activo
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Endpoints de registro, login y validacion de tokens JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registrar")
    @Operation(summary = "Registrar credenciales", description = "Crea las credenciales (email + password) para un usuario ya existente en MS-USUARIOS")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Autentica con email y password, devuelve JWT Bearer token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/validar")
    @Operation(summary = "Validar token JWT", description = "Verifica si un token JWT es valido y no ha expirado")
    public ResponseEntity<ValidarTokenResponse> validarToken(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(
                    ValidarTokenResponse.builder().valido(false)
                            .mensaje("Header invalido. Formato esperado: Bearer <token>").build());
        }

        String token = authHeader.substring(7);
        ValidarTokenResponse response = authService.validarToken(token);
        return response.isValido() ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica que el microservicio de seguridad esta activo")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("servicio", "ms-seguridad", "estado", "activo", "puerto", "8082"));
    }
}
