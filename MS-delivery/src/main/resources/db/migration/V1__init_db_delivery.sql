-- ============================================================
-- Flyway Migration V1 — MS-DELIVERY
-- Base de datos: db_delivery
-- Tablas: repartidor, delivery, ubicacion_historial
--
-- NOTA: columnas de coordenadas y medidas usan DOUBLE
-- porque Hibernate 6.6 mapea Java Double → FLOAT/DOUBLE.
-- Usar DECIMAL causa SchemaValidationException con ddl-auto=validate.
-- ============================================================

-- ------------------------------------------------------------
-- 1. REPARTIDOR
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS repartidor (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(100) NOT NULL,
    telefono         VARCHAR(15)  NOT NULL,
    email            VARCHAR(150) NOT NULL,
    vehiculo         ENUM('MOTO','BICICLETA','AUTO','A_PIE') NOT NULL DEFAULT 'MOTO',
    estado           ENUM('LIBRE','OCUPADO','INACTIVO')      NOT NULL DEFAULT 'LIBRE',
    latitud          DOUBLE       NULL,
    longitud         DOUBLE       NULL,
    ultima_ubicacion DATETIME     NULL,
    activo           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_repartidor_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 2. DELIVERY
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS delivery (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    pedido_id         BIGINT       NOT NULL,
    repartidor_id     BIGINT       NOT NULL,
    direccion_destino VARCHAR(255) NOT NULL,
    latitud_destino   DOUBLE       NULL,
    longitud_destino  DOUBLE       NULL,
    estado            ENUM('PENDIENTE','EN_RUTA','ENTREGADO','FALLIDO','CANCELADO') NOT NULL DEFAULT 'PENDIENTE',
    intentos          INT          NOT NULL DEFAULT 0,
    notas             TEXT         NULL,
    fecha_asignacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio_ruta DATETIME     NULL,
    fecha_entrega     DATETIME     NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_repartidor
        FOREIGN KEY (repartidor_id) REFERENCES repartidor (id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 3. UBICACION_HISTORIAL
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ubicacion_historial (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    repartidor_id BIGINT   NOT NULL,
    delivery_id   BIGINT   NULL,
    latitud       DOUBLE   NOT NULL,
    longitud      DOUBLE   NOT NULL,
    velocidad_kmh DOUBLE   NULL,
    precision_m   DOUBLE   NULL,
    timestamp     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_historial_repartidor
        FOREIGN KEY (repartidor_id) REFERENCES repartidor (id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_historial_delivery
        FOREIGN KEY (delivery_id) REFERENCES delivery (id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;