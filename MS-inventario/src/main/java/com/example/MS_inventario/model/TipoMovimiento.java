package com.example.MS_inventario.model;

/**
 * Tipos de movimiento de stock.
 *
 * ENTRADA   → ingreso de mercaderia (solo ADMIN)
 * SALIDA    → descuento de stock al confirmar un pedido (solo ADMIN)
 * AJUSTE    → correccion manual de inventario (solo ADMIN)
 */
public enum TipoMovimiento {
    ENTRADA,
    SALIDA,
    AJUSTE
}
