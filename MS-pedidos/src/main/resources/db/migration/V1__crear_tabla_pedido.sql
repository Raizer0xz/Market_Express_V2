CREATE TABLE IF NOT EXISTS pedido (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id         BIGINT         NOT NULL,
    sucursal_id        BIGINT         NOT NULL,
    carrito_id         BIGINT,
    estado             VARCHAR(20)    NOT NULL,
    total              DECIMAL(10,2)  NOT NULL,
    direccion_entrega  VARCHAR(255)   NOT NULL,
    created_at         DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO pedido (
    usuario_id,
    sucursal_id,
    carrito_id,
    estado,
    total,
    direccion_entrega,
    created_at,
    updated_at
) VALUES (
    1,                       -- usuario_id
    10,                      -- sucursal_id
    500,                     -- carrito_id
    'PENDIENTE',             -- estado (usando String por EnumType.STRING)
    1550.50,                 -- total (BigDecimal)
    'Av. Siempreviva 742, Springfield', -- direccion_entrega
    NOW(),                   -- created_at
    NOW()                    -- updated_at
);