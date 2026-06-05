package com.example.MS_pedidos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - MS-Pedidos
 *
 * Captura:
 *   - MethodArgumentNotValidException: errores de @Valid en el body del pedido
 *   - MethodArgumentTypeMismatchException: EstadoPedido invalido en @RequestParam
 *   - RuntimeException: errores de negocio
 *   - Exception: cualquier error inesperado -> 500
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Captura errores de @Valid.
     * Ejemplo: usuarioId null, direccionEntrega vacia, total negativo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });
        log.warn("Error de validacion en MS-pedidos: {}", errores);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "error", "Validacion fallida",
                        "campos", errores
                ));
    }

    /**
     * Captura errores de conversion de tipo.
     * Ejemplo: ?estado=INVALIDO cuando EstadoPedido no tiene ese valor.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensaje = String.format("Valor '%s' no valido para el parametro '%s'",
                ex.getValue(), ex.getName());
        log.warn("Error de tipo en MS-pedidos: {}", mensaje);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", 400, "error", mensaje));
    }

    /**
     * Captura RuntimeException de negocio.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException en MS-pedidos: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", 400, "error", ex.getMessage()));
    }

    /**
     * Captura errores inesperados -> 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Error inesperado en MS-pedidos: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", 500, "error", "Error interno del servidor"));
    }
}