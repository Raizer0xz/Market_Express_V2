-- Tabla principal: stock por producto y sucursal
CREATE TABLE IF NOT EXISTS inventario (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id  BIGINT NOT NULL COMMENT 'FK logica a MS-productos',
    sucursal_id  BIGINT NOT NULL COMMENT 'FK logica a MS-sucursales',
    cantidad     INT    NOT NULL DEFAULT 0,
    stock_minimo INT    NOT NULL DEFAULT 5 COMMENT 'Umbral de alerta de stock bajo',
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_producto_sucursal UNIQUE (producto_id, sucursal_id)
);

-- Historial de movimientos de stock
CREATE TABLE IF NOT EXISTS movimientos_inventario (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id      BIGINT       NOT NULL,
    sucursal_id      BIGINT       NOT NULL,
    tipo             VARCHAR(20)  NOT NULL COMMENT 'ENTRADA | SALIDA | AJUSTE',
    cantidad         INT          NOT NULL COMMENT 'Cantidad del movimiento (siempre positivo)',
    stock_resultante INT          NOT NULL COMMENT 'Stock despues de aplicar el movimiento',
    motivo           VARCHAR(300) COMMENT 'Descripcion del motivo del movimiento',
    usuario_id       BIGINT       COMMENT 'Admin que realizo el movimiento',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
