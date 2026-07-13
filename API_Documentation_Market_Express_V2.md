# Documentación de Endpoints — Market Express V2

## Arquitectura general

```
Cliente → Gateway (5050) → Eureka (8761) → Microservicio
```

Todo pasa por el gateway. El JWT se valida ahí y se inyectan los headers:
- `X-Usuario-Id`, `X-Usuario-Email`, `X-Usuario-Rol`

---

## MS-SEGURIDAD — puerto 8082
**Swagger:** `http://localhost:8082/swagger-ui/index.html`

| Método | URL | Descripción | Auth |
|--------|-----|-------------|------|
| POST | `/auth/registrar` | Registrar credenciales (email + password) | No |
| POST | `/auth/login` | Login → devuelve JWT | No |
| POST | `/auth/validar` | Validar token JWT | Bearer token |
| GET | `/auth/health` | Health check | No |

**Body `/auth/registrar`:**
```json
{ "usuarioId": 1, "email": "juan@mail.com", "password": "segura123", "rol": "CLIENTE" }
```
**Body `/auth/login`:**
```json
{ "email": "juan@mail.com", "password": "segura123" }
```
**Respuesta login/registrar:**
```json
{ "token": "eyJ...", "email": "juan@mail.com", "rol": "CLIENTE", "usuarioId": 1 }
```

---

##  MS-USUARIOS — puerto 8084
**Swagger:** `http://localhost:8084/swagger-ui/index.html`

| Método | URL | Descripción | Auth |
|--------|-----|-------------|------|
| POST | `/api/v1/usuarios` | Crear usuario | Bearer token |
| GET | `/api/v1/usuarios` | Listar todos | Bearer token |
| GET | `/api/v1/usuarios/{id}` | Buscar por ID | Bearer token |
| GET | `/api/v1/usuarios/email/{email}` | Buscar por email | Bearer token |
| GET | `/api/v1/usuarios/rol/{rol}` | Listar por rol | Bearer token |
| PUT | `/api/v1/usuarios/{id}` | Actualizar usuario | Bearer token |
| DELETE | `/api/v1/usuarios/{id}` | Eliminar usuario | Bearer token |
| GET | `/api/v1/usuarios/health` | Health check | No |

**Body POST/PUT:**
```json
{ "nombre": "Juan Pérez", "email": "juan@mail.com", "passwordHash": "hash", "telefono": "912345678", "rol": "CLIENTE" }
```

**HATEOAS links:** `self`, `todos`, `update`, `delete`

---

##  MS-PRODUCTOS — puerto 8085
**Swagger:** `http://localhost:8085/swagger-ui/index.html`

### Productos
| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v2/productos` | Listar productos activos |
| GET | `/api/v2/productos/{id}` | Buscar por ID |
| GET | `/api/v2/productos/categoria/{catId}` | Listar por categoría |
| GET | `/api/v2/productos/buscar?nombre=X` | Buscar por nombre |
| POST | `/api/v2/productos` | Crear producto |
| PUT | `/api/v2/productos/{id}` | Actualizar producto |
| DELETE | `/api/v2/productos/{id}` | Desactivar (baja lógica) |

**Body POST producto:**
```json
{ "nombre": "Leche Entera", "descripcion": "1L", "unidadMedida": "litro", "categoria": { "id": 1 } }
```
**HATEOAS links producto:** `self`, `todos`, `update`, `desactivar`, `categoria`, `precios`

### Categorías
| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v2/categorias` | Listar todas |
| GET | `/api/v2/categorias/{id}` | Buscar por ID |
| POST | `/api/v2/categorias` | Crear categoría |
| PUT | `/api/v2/categorias/{id}` | Actualizar categoría |
| DELETE | `/api/v2/categorias/{id}` | Eliminar (falla si tiene productos) |

**HATEOAS links categoría:** `self`, `todas`, `update`, `delete`, `productos`

### Precios
| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v2/precios/producto/{productoId}` | Precios de un producto |
| GET | `/api/v2/precios/producto/{productoId}/sucursal/{sucursalId}` | Precio por producto y sucursal |
| POST | `/api/v2/precios` | Registrar precio |
| DELETE | `/api/v2/precios/{id}` | Eliminar precio |

**Body POST precio:**
```json
{ "producto": { "id": 1 }, "sucursalId": 2, "precio": 1500.00 }
```
**HATEOAS links precio:** `self`, `delete`, `producto`

---

##  MS-CARRITO — puerto 8086
**Swagger:** `http://localhost:8086/swagger-ui/index.html`

| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/v2/carritos` | Crear carrito |
| GET | `/api/v2/carritos/activo/{usuarioId}` | Obtener carrito activo del usuario |
| GET | `/api/v2/carritos/{id}/items` | Listar ítems del carrito |
| GET | `/api/v2/carritos/{id}/items/detalle` | Ítems con nombre del producto (Feign) |
| GET | `/api/v2/carritos/{id}/total` | Calcular total |
| POST | `/api/v2/carritos/{id}/items` | Agregar ítem |
| DELETE | `/api/v2/carritos/items/{itemId}` | Eliminar ítem |
| DELETE | `/api/v2/carritos/{id}` | Eliminar carrito completo |
| PUT | `/api/v2/carritos/{id}/confirmar` | Confirmar carrito |

**Body crear carrito:**
```json
{ "usuarioId": 1, "sucursalId": 2 }
```
**Body agregar ítem:**
```json
{ "productoId": 5, "cantidad": 2, "precioUnitario": 1500.00 }
```
**HATEOAS links carrito:** `self`, `items`, `items-detalle`, `total`, `confirmar`, `delete`

---

##  MS-SUCURSALES — puerto 8087
**Swagger:** `http://localhost:8087/swagger-ui/index.html`

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v1/sucursales` | Listar todas |
| GET | `/api/v1/sucursales/abiertas` | Solo sucursales abiertas |
| GET | `/api/v1/sucursales/{id}` | Buscar por ID |
| POST | `/api/v1/sucursales` | Crear sucursal |
| PUT | `/api/v1/sucursales/{id}` | Actualizar sucursal |
| PATCH | `/api/v1/sucursales/{id}/estado?abierta=true` | Abrir/cerrar sucursal |
| DELETE | `/api/v1/sucursales/{id}` | Eliminar sucursal |
| GET | `/api/v1/sucursales/health` | Health check |

**Body POST/PUT:**
```json
{ "nombre": "Sucursal Centro", "direccion": "Av. Libertador 123", "latitud": -33.4489, "longitud": -70.6693, "horarioApertura": "08:00", "horarioCierre": "22:00" }
```
**HATEOAS links:** `self`, `todas`, `update`, `delete`, `cambiar-estado`

---

##  MS-PEDIDOS — puerto 8088
**Swagger:** `http://localhost:8088/swagger-ui/index.html`

| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v1/pedidos` | Listar todos |
| GET | `/api/v1/pedidos/usuario/{usuarioId}` | Pedidos de un usuario |
| GET | `/api/v1/pedidos/sucursal/{sucursalId}?estado=PENDIENTE` | Pedidos por sucursal y estado |
| POST | `/api/v1/pedidos` | Crear pedido |
| PUT | `/api/v1/pedidos/{id}/estado?nuevoEstado=CONFIRMADO` | Cambiar estado |
| DELETE | `/api/v1/pedidos/{id}` | Eliminar pedido |

**Body POST:**
```json
{ "usuarioId": 1, "sucursalId": 10, "carritoId": 500, "estado": "PENDIENTE", "total": 1550.50, "direccionEntrega": "Av. Siempreviva 742" }
```
**Estados:** `PENDIENTE`, `CONFIRMADO`, `PREPARANDO`, `EN_CAMINO`, `ENTREGADO`, `CANCELADO`

**HATEOAS links:** `todos`, `update`, `delete`

---

##  MS-PAGOS — puerto 9091
**Swagger:** `http://localhost:9091/swagger-ui/index.html`

| Método | URL | Descripción | Rol requerido |
|--------|-----|-------------|---------------|
| POST | `/api/v1/pagos/procesar` | Procesar pago | CLIENTE o ADMIN |
| GET | `/api/v1/pagos/metodos` | Métodos disponibles | Cualquiera |
| GET | `/api/v1/pagos/pedido/{pedidoId}` | Pagos de un pedido | Cualquiera |
| POST | `/api/v1/pagos/confirmar?transaccionId=X&status=SUCCESS` | Confirmar transacción | Solo ADMIN |
| GET | `/api/v1/pagos/health` | Health check | No |

**Body `/pagos/procesar`:**
```json
{ "pedidoId": 1, "monto": 15000.00, "moneda": "CLP", "metodo": "TARJETA_CREDITO" }
```
**Métodos:** `TARJETA_CREDITO`, `TARJETA_DEBITO`, `TRANSFERENCIA_BANCARIA`, `PAYPAL`

**HATEOAS links:** `pagos-del-pedido`, `metodos-disponibles`, `confirmar`

---

##  MS-INVENTARIO — puerto 8089
**Swagger:** `http://localhost:8089/swagger-ui/index.html`

| Método | URL | Descripción | Rol requerido |
|--------|-----|-------------|---------------|
| GET | `/api/v1/inventario/sucursal/{sucursalId}` | Stock de una sucursal | Cualquiera |
| GET | `/api/v1/inventario/producto/{productoId}` | Stock de un producto | Cualquiera |
| GET | `/api/v1/inventario/producto/{productoId}/sucursal/{sucursalId}` | Stock exacto | Cualquiera |
| GET | `/api/v1/inventario/alertas` | Alertas globales stock bajo | Cualquiera |
| GET | `/api/v1/inventario/alertas/sucursal/{sucursalId}` | Alertas por sucursal | Cualquiera |
| POST | `/api/v1/inventario/aumentar` | Aumentar stock | Solo ADMIN |
| POST | `/api/v1/inventario/reducir` | Reducir stock | Solo ADMIN |
| POST | `/api/v1/inventario/ajustar` | Ajustar stock a valor exacto | Solo ADMIN |
| PUT | `/api/v1/inventario/stock-minimo?productoId=X&sucursalId=Y&stockMinimo=Z` | Actualizar umbral alerta | Solo ADMIN |
| GET | `/api/v1/inventario/historial/sucursal/{sucursalId}` | Historial completo | Solo ADMIN |
| GET | `/api/v1/inventario/historial/producto/{productoId}/sucursal/{sucursalId}` | Historial por producto | Solo ADMIN |
| GET | `/api/v1/inventario/historial/sucursal/{sucursalId}/tipo/{tipo}` | Historial por tipo | Solo ADMIN |

**Body aumentar/reducir:**
```json
{ "productoId": 10, "sucursalId": 2, "cantidad": 20, "motivo": "Recepción proveedor" }
```
**Body ajustar:**
```json
{ "productoId": 10, "sucursalId": 2, "nuevaCantidad": 35, "motivo": "Conteo físico" }
```
**Tipos movimiento:** `ENTRADA`, `SALIDA`, `AJUSTE`

**HATEOAS links:** `self`, `sucursal`, `producto`, `alertas`

---

##  MS-DELIVERY — puerto 9090
**Swagger:** `http://localhost:9090/swagger-ui/index.html`

### Delivery
| Método | URL | Descripción |
|--------|-----|-------------|
| POST | `/api/v1/delivery/asignar` | Asignar delivery al repartidor más cercano |
| GET | `/api/v1/delivery/{id}` | Obtener delivery por ID |
| GET | `/api/v1/delivery/pedido/{pedidoId}` | Delivery activo de un pedido |
| GET | `/api/v1/delivery/estado/{estado}` | Listar por estado |
| PUT | `/api/v1/delivery/{id}/iniciar-ruta` | PENDIENTE → EN_RUTA |
| PUT | `/api/v1/delivery/{id}/entregar` | EN_RUTA → ENTREGADO |
| PUT | `/api/v1/delivery/{id}/fallo` | Reportar fallo |
| PUT | `/api/v1/delivery/{id}/cancelar` | Cancelar delivery |
| GET | `/api/v1/delivery/pedido/{pedidoId}/ubicacion` | Ubicación GPS en tiempo real |
| GET | `/api/v1/delivery/{id}/ruta` | Ruta GPS completa recorrida |
| GET | `/api/v1/delivery/repartidor/{repId}/historial` | Historial del repartidor |

**Estados delivery:** `PENDIENTE`, `EN_RUTA`, `ENTREGADO`, `FALLIDO`, `CANCELADO`

**HATEOAS links delivery:** `self`, `ruta`, `pedido`, `historial-repartidor`

### Repartidores
| Método | URL | Descripción |
|--------|-----|-------------|
| GET | `/api/v1/repartidores` | Listar activos |
| GET | `/api/v1/repartidores/{id}` | Buscar por ID |
| POST | `/api/v1/repartidores` | Registrar repartidor |
| PUT | `/api/v1/repartidores/{id}` | Actualizar datos |
| DELETE | `/api/v1/repartidores/{id}` | Desactivar (baja lógica) |
| PATCH | `/api/v1/repartidores/{id}/estado?estado=LIBRE` | Cambiar estado |
| POST | `/api/v1/repartidores/{id}/ubicacion` | Actualizar GPS |
| GET | `/api/v1/repartidores/{id}/metricas` | Ver métricas |

**Estados repartidor:** `LIBRE`, `OCUPADO`, `INACTIVO`
**Vehículos:** `MOTO`, `AUTO`, `BICICLETA`, `CAMIONETA`

**HATEOAS links repartidor:** `self`, `todos`, `update`, `desactivar`, `metricas`, `historial-deliveries`

---

## Orden de inicio

```
1. MS-eureka-server     → http://localhost:8761
2. MS-gateway-security  → http://localhost:8082
3. MS-gateway           → http://localhost:5050
4. Resto en cualquier orden
```

## Acceso via Gateway (puerto 5050)

Todas las rutas son accesibles via `http://localhost:5050` + la URL del endpoint.
Ejemplo: `http://localhost:5050/api/v1/usuarios` en vez de `http://localhost:8084/api/v1/usuarios`

