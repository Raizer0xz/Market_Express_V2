package com.example.MS_productos.config;

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
 * GlobalExceptionHandler - MS-Productos
 *
 * Captura:
 *   - MethodArgumentNotValidException: errores de @Valid (@NotBlank, @Size, @Positive, etc.)
 *   - RuntimeException: errores de negocio (producto no encontrado, categoria invalida)
 *   - IllegalArgumentException: errores de argumentos invalidos
 *   - Exception: cualquier error inesperado -> 500
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Captura errores de validacion de @Valid.
     * Devuelve mapa campo -> mensaje.
     * Ejemplo: { "nombre": "El nombre es obligatorio", "precio": "El precio debe ser mayor a 0" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.warn("Error de validacion en MS-productos: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Validacion fallida",
                        "campos", errores
                ));
    }

    /**
     * Captura RuntimeException (producto no encontrado, categoria no encontrada).
     * Nota: los controllers que lanzaban 404 manualmente con try/catch
     * ahora pueden simplificarse lanzando RuntimeException directamente.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException en MS-productos: {}", ex.getMessage());
        // Si el mensaje indica "no encontrado", devolver 404
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
        log.error("Error inesperado en MS-productos: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", 500, "error", "Error interno del servidor"));
    }
}