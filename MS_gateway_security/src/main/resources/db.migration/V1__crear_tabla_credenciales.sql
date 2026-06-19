DROP TABLE IF EXISTS credenciales;

CREATE TABLE credenciales (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL UNIQUE,        -- referencia al ms-usuarios
    email       VARCHAR(255) NOT NULL UNIQUE,        -- coincide con Credencial.email
    password_hash VARCHAR(255) NOT NULL,             -- coincide con Credencial.passwordHash
    rol         VARCHAR(50)  NOT NULL DEFAULT 'CLIENTE',
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
