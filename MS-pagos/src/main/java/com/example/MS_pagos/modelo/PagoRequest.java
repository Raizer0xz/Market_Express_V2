package com.example.MS_pagos.modelo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * PagoRequest - DTO para crear un pago.
 *
 * FIX: Se agregaron validaciones con jakarta.validation para evitar:
 *   - NullPointerException cuando metodo es null (se llamaba .toUpperCase() en null)
 *   - Pagos con monto 0 o negativo
 *   - Pagos sin pedidoId
 *
 * FIX: monto cambiado de Double a BigDecimal para evitar errores de precision
 * con valores monetarios (ej: 1999.99 puede ser 1999.9899999... con Double).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoRequest {

    @NotNull(message = "El pedidoId es obligatorio")
    private Long pedidoId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal monto;  // FIX: era Double, ahora BigDecimal

    @Size(max = 10, message = "La moneda no puede superar 10 caracteres")
    private String moneda;     // Opcional: si es null se usa "CLP" en el service

    @NotBlank(message = "El metodo de pago es obligatorio")
    @Size(max = 50, message = "El metodo no puede superar 50 caracteres")
    private String metodo;     // Debe coincidir con MetodoPago enum (EFECTIVO, DEBITO, CREDITO, etc.)
}