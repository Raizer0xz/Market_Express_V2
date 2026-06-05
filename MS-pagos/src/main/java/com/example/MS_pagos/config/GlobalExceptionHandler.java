package com.example.MS_pagos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - MS-Pagos
 *
 * Captura:
 *   - MethodArgumentNotValidException: errores de @Valid en PagoRequest
 *   - IllegalArgumentException: metodo de pago invalido (MetodoPago.valueOf falla)
 *   - RuntimeException: transaccion no encontrada, etc.
 *   - Exception: cualquier error inesperado -> 500
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Captura errores de @Valid en PagoRequest.
     * Ejemplo: monto null, metodo vacio, pedidoId null.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.warn("Error de validacion en MS-pagos: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Validacion fallida",
                        "campos", errores
                ));
    }

    /**
     * Captura IllegalArgumentException lanzada cuando el metodo de pago
     * no existe en el enum MetodoPago.
     * Ejemplo: metodo="PAYPAL" cuando MetodoPago solo tiene EFECTIVO, DEBITO, etc.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento invalido en MS-pagos: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", 400, "error", ex.getMessage()));
    }

    /**
     * Captura RuntimeException (transaccion no encontrada, etc.).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException en MS-pagos: {}", ex.getMessage());
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("no encontrado")) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", 404, "error", ex.getMessage()));
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", 400, "error", ex.getMessage()));
    }

    /**
     * Captura errores inesperados -> 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error inesperado en MS-pagos: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", 500, "error", "Error interno del servidor"));
    }
}