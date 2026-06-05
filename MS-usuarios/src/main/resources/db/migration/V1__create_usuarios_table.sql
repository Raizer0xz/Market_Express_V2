CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    telefono VARCHAR(15),
    rol VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Opcional: Insertar un administrador por defecto para probar
INSERT INTO usuarios (nombre, email, password_hash, rol)
VALUES ('Admin Market', 'admin@marketexpress.com', 'admin123', 'ADMIN');