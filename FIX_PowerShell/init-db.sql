-- ============================================================
-- init-db.sql — Market Express V2
-- Crea todas las bases de datos necesarias para los microservicios
-- ============================================================

CREATE DATABASE IF NOT EXISTS `MS-usuarios`    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `MS-productos`   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `MS-carrito`     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `MS-sucursales`  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `MS-pedidos`     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `MS-inventario`  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `MS-pagos`       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `MS-delivery`    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `MS-SEGURIDAD`   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Dar permisos al usuario
GRANT ALL PRIVILEGES ON `MS-usuarios`.*   TO 'user'@'%';
GRANT ALL PRIVILEGES ON `MS-productos`.*  TO 'user'@'%';
GRANT ALL PRIVILEGES ON `MS-carrito`.*    TO 'user'@'%';
GRANT ALL PRIVILEGES ON `MS-sucursales`.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON `MS-pedidos`.*    TO 'user'@'%';
GRANT ALL PRIVILEGES ON `MS-inventario`.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON `MS-pagos`.*      TO 'user'@'%';
GRANT ALL PRIVILEGES ON `MS-delivery`.*   TO 'user'@'%';
GRANT ALL PRIVILEGES ON `MS-SEGURIDAD`.*  TO 'user'@'%';

FLUSH PRIVILEGES;
