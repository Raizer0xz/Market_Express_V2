CREATE TABLE IF NOT EXISTS categoria (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    imagen_url  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS producto (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria_id  BIGINT       NOT NULL,
    nombre        VARCHAR(100) NOT NULL,
    descripcion   VARCHAR(255),
    imagen_url    VARCHAR(255),
    unidad_medida VARCHAR(50),
    activo        TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);

CREATE TABLE IF NOT EXISTS precio_producto (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id   BIGINT         NOT NULL,
    sucursal_id   BIGINT         NOT NULL,
    precio        DECIMAL(10, 2) NOT NULL,
    vigente_desde DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_precio_producto FOREIGN KEY (producto_id) REFERENCES producto(id)
);

-- Datos de prueba
INSERT INTO categoria (nombre, descripcion) VALUES
('Frutas y Verduras', 'Productos frescos del campo'),
('Lácteos', 'Leche, quesos y derivados'),
('Bebidas', 'Jugos, aguas y bebidas varias');

INSERT INTO producto (categoria_id, nombre, descripcion, unidad_medida, activo) VALUES
(1, 'Manzana', 'Manzana roja importada', 'kg', 1),
(1, 'Tomate', 'Tomate fresco nacional', 'kg', 1),
(2, 'Leche Entera', 'Leche entera 1 litro', 'unidad', 1);