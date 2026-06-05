package com.example.MS_carrito.config;

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
 * GlobalExceptionHandler - MS-Carrito
 *
 * Captura:
 *   - MethodArgumentNotValidException: errores de @Valid en ItemCarrito
 *     (cantidad < 1, precioUnitario negativo, etc.)
 *   - RuntimeException: carrito no encontrado, usuario ya tiene carrito activo, etc.
 *   - Exception: cualquier error inesperado -> 500
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Captura errores de validacion (@Valid).
     * Ejemplo: cantidad=0 falla @Min(1), precioUnitario negativo falla @Positive.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.warn("Error de validacion en MS-carrito: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Validacion fallida",
                        "campos", errores
                ));
    }

    /**
     * Captura RuntimeException del service.
     * Ejemplos: "No hay carrito activo", "El usuario ya tiene un carrito activo",
     * "Carrito no encontrado", "Item no encontrado".
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException en MS-carrito: {}", ex.getMessage());
        // Distinguir 404 vs 400 por el mensaje
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
        log.error("Error inesperado en MS-carrito: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", 500, "error", "Error interno del servidor"));
    }
}