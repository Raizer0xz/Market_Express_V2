# Pasos para levantar Market Express V2 con Docker

## 1. Compilar todos los microservicios (CMD)

```
cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-eureka-server
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-gateway
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS_gateway_security
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-usuarios
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-productos
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-carrito
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-sucursales
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-pedidos
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-inventario
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-pagos
mvnw clean package -DskipTests

cd C:\Users\Raizer\Downloads\Market_Express_V2\MS-delivery
mvnw clean package -DskipTests
```

## 2. Levantar Docker

```
cd C:\Users\Raizer\Downloads\Market_Express_V2
docker-compose up --build
```

## 3. Apagar Docker

```
docker-compose down
```
