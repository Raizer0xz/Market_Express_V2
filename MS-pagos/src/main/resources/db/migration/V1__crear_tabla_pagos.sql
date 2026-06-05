CREATE TABLE IF NOT EXISTS pagos (
    id                 BIGINT         AUTO_INCREMENT PRIMARY KEY,
    pedido_id          BIGINT         NOT NULL COMMENT 'FK logica a MS-pedidos',
    transaccion_id     VARCHAR(100)   UNIQUE COMMENT 'UUID generado en procesarPago',
    monto              FLOAT(10, 2) NOT NULL,
    moneda             VARCHAR(10)    NOT NULL DEFAULT 'CLP',
    metodo             VARCHAR(50)    NOT NULL COMMENT 'EFECTIVO | DEBITO | CREDITO | TRANSFERENCIA',
    estado             VARCHAR(30)    NOT NULL COMMENT 'PROCESANDO | COMPLETADO | RECHAZADO',
    confirmacion_hash  VARCHAR(255)   COMMENT 'Hash de confirmacion del procesador externo',
    fecha_creacion     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Dato de prueba: pago procesado para el pedido de ejemplo
INSERT INTO pagos (pedido_id, transaccion_id, monto, moneda, metodo, estado)
VALUES (1, UUID(), 1550.50, 'CLP', 'DEBITO', 'COMPLETADO');