package com.example.MS_usuarios.config;

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
 * GlobalExceptionHandler - MS-Usuarios
 * Centraliza el manejo de errores para devolver respuestas JSON claras.
 *
 * Captura:
 *   - MethodArgumentNotValidException: errores de @Valid (@NotBlank, @Email, etc.)
 *   - RuntimeException: errores de negocio (usuario no encontrado, etc.)
 *   - Exception: cualquier error inesperado -> 500
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Captura errores de validacion (@Valid en controller).
     * Devuelve un mapa con campo -> mensaje de error.
     * Ejemplo: { "email": "Formato de email invalido", "nombre": "El nombre es obligatorio" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.warn("Error de validacion en MS-usuarios: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Validacion fallida",
                        "campos", errores
                ));
    }

    /**
     * Captura RuntimeException del service (ej: usuario no encontrado).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException en MS-usuarios: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", ex.getMessage()
                ));
    }

    /**
     * Captura cualquier error inesperado -> 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error inesperado en MS-usuarios: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "error", "Error interno del servidor"
                ));
    }
}