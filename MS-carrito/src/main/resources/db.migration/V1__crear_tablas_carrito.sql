CREATE TABLE IF NOT EXISTS carrito (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL,
    sucursal_id BIGINT       NOT NULL,
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS item_carrito (
    id              BIGINT         AUTO_INCREMENT PRIMARY KEY,
    carrito_id      BIGINT         NOT NULL,
    producto_id     BIGINT         NOT NULL,
    cantidad        INT            NOT NULL CHECK (cantidad >= 1),
    precio_unitario DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_item_carrito FOREIGN KEY (carrito_id) REFERENCES carrito(id)
);