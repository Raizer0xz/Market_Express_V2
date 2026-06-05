CREATE TABLE sucursales (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    direccion        VARCHAR(255) NOT NULL,
    latitud          DOUBLE       NOT NULL,
    longitud         DOUBLE       NOT NULL,
    horario_apertura VARCHAR(5)   NOT NULL, -- Formato HH:mm
    horario_cierre   VARCHAR(5)   NOT NULL, -- Formato HH:mm
    abierta          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Sucursal de prueba (Santiago centro, coordenadas reales)
INSERT INTO sucursales (nombre, direccion, latitud, longitud, horario_apertura, horario_cierre)
VALUES ('Market Express Central', 'Av. Principal 123, Santiago', -33.4489, -70.6693, '08:00', '22:00');