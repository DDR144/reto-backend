# Order Management Platform (OMP)

Reto técnico backend: plataforma de pedidos con arquitectura de microservicios,
expuesta a través de un API Gateway con autenticación OAuth 2.0/OIDC.

## Arquitectura

```
                    +----------------+
   Postman/Client   |  API Gateway   |  (Spring Cloud Gateway, reactivo)
   -------------->  |  :8080         |  - enrutamiento
                    +-------+--------+  - validación JWT
                            |            - generación/propagación X-Trace-Id
              +-------------+-------------+
              |                           |
   +----------v-----------+   +-----------v-----------+
   |    Order Service     |   |   Inventory Service    |
   |    :8081             |   |   :8082                |
   |  - pedidos           |   |  - productos/stock     |
   |  - historial         |   |  - validación atómica  |
   |  - cancelación       |   +-----------+-----------+
   +----------+-----------+               |
              |        WebClient (reactivo) |
              +-----------------------------+
                            |
                   +--------v--------+
                   |   PostgreSQL    |  (una BD por servicio)
                   |   :5432         |
                   +-----------------+

   Keycloak :8083  -> emisión y validación de JWT (realm reto-backend)
```

## Componentes

| Componente | Puerto (host) | Responsabilidad |
|---|---|---|
| API Gateway | 8080 | Único punto de entrada, enrutamiento, validación JWT, propagación de X-Trace-Id. Sin lógica de negocio. |
| Order Service | 8081 | Creación, consulta de estado, historial y cancelación de pedidos. |
| Inventory Service | 8082 | Gestión de productos y stock, validación/descuento atómico de stock. |
| Keycloak | 8083 | Servidor de autenticación OAuth 2.0/OIDC. |
| PostgreSQL (order-db) | 5434 | Persistencia del Order Service (pedidos, historial). |
| PostgreSQL (inventory-db) | 5433 | Persistencia del Inventory Service (productos, stock). |

## Requisitos

- Docker y Docker Compose.
- Java 17+ y Maven 3.9+ (solo si se compila fuera de Docker).

## Puesta en marcha

```bash
docker compose up --build
```

Esperar a que `postgres` y `keycloak` estén `healthy`. Después:

- API Gateway: `http://localhost:8080`
- Swagger UI Order Service: `http://localhost:8081/swagger-ui.html`
- Swagger UI Inventory Service: `http://localhost:8082/swagger-ui.html`
- Keycloak admin: `http://localhost:8083` (admin / admin)

## Compilación (sin Docker)

No hay un POM agregador en la raíz: **cada módulo se construye por separado**,
desde su propio directorio y con su propio wrapper de Maven.

```bash
cd api-gateway          && ./mvnw clean package
cd ../order-service     && ./mvnw clean package
cd ../inventory-service && ./mvnw clean package
```

Cada módulo deja su JAR en `<módulo>/target/`. La compilación no necesita Docker,
Keycloak ni PostgreSQL en ejecución: las pruebas usan repositorios y clientes
mockeados.

## Autenticación

Realm importado automáticamente: `reto-backend`.

- Cliente confidencial: `reto-api-client` (secret `reto-api-client-secret`).
- Usuario de prueba: `testuser` / `password`.

Obtener token (password grant):

```bash
curl -s -X POST http://localhost:8083/realms/reto-backend/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=reto-api-client&client_secret=reto-api-client-secret&username=testuser&password=password'
```

Todas las rutas de negocio exigen `Authorization: Bearer <token>`.

## Endpoints (a través del Gateway, `http://localhost:8080`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/orders` | Registrar pedido (valida stock y confirma). |
| GET | `/api/v1/orders/{orderId}` | Consultar estado del pedido. |
| GET | `/api/v1/orders/{orderId}/history` | Consultar historial de cambios de estado. |
| POST | `/api/v1/orders/{orderId}/cancel` | Cancelar pedido (si el estado lo permite). |
| GET | `/api/v1/products/{productId}` | Consultar producto y stock. |
| POST | `/api/v1/products/validate` | Validar/descontar stock (uso interno del Order Service). |

## Estados y transiciones de pedido

```
PENDING  -> CONFIRMED   (stock disponible)
PENDING  -> CANCELLED
CONFIRMED -> CANCELLED
CANCELLED -> (terminal)
```

## Formato de error

```json
{ "timestamp": "...", "status": 409, "code": "STOCK_INSUFFICIENT", "message": "...", "traceId": "..." }
```

Códigos: `UNAUTHORIZED`, `STOCK_INSUFFICIENT`, `INVALID_TRANSITION`,
`ORDER_NOT_FOUND`, `PRODUCT_NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_ERROR`.

## Trazabilidad (X-Trace-Id)

- El Gateway genera un UUID v4 si la petición no trae `X-Trace-Id`; si lo trae, lo propaga (decisión: ambos).
- Se propaga a Order e Inventory Service y se incluye en cada línea de log (Logback, MDC) y en las respuestas de error.
- La cabecera se devuelve en la respuesta para correlacionar.

## Documentación y pruebas

- OpenAPI generado (Swagger UI) en cada microservicio y spec estático en `docs/openapi.yaml`.
- Colección Postman en `postman/OMP.postman_collection.json`.

### Pruebas unitarias (JUnit 5 + Mockito + StepVerifier)

```bash
cd api-gateway       && ./mvnw test
cd ../order-service  && ./mvnw test
cd ../inventory-service && ./mvnw test
```

- `ApiGatewayApplicationTests`: carga de contexto del gateway (1 caso).
- `OrderStatusTest`: máquina de estados de pedido (6 casos).
- `OrderServiceTest`: creación/consulta/cancelación con repositorios y cliente mockeados (8 casos).
- `InventoryControllerTest`: validación de stock y consulta de producto (5 casos).

Total: **20 casos de prueba**, todos offline (sin Docker ni servicios externos).

### Escenarios de prueba (Postman)

1. Crear pedido con stock disponible → 201 CONFIRMED.
2. Crear pedido sin stock → 409 STOCK_INSUFFICIENT (pedido no confirmado).
3. Cancelar pedido en estado válido → 200 CANCELLED.
4. Cancelar pedido ya cancelado → 409 INVALID_TRANSITION.
5. Sin token → 401 con traceId.
6. Verificar el mismo X-Trace-Id en logs del Gateway y de ambos microservicios.

## Decisiones de diseño

| Decisión | Elección |
|---|---|
| Base de datos | PostgreSQL |
| Comunicación entre servicios | Spring WebFlux completo (WebClient), no bloqueante |
| Gestión de estados | Enum simple persistido como STRING |
| X-Trace-Id | Ambos: heredar header del cliente o generar UUID en el gateway |
| Validación de stock | UPDATE atómico condicional (`stock >= quantity`) |
| IDs | UUID v4 |
| Autenticación | Keycloak (imagen oficial Docker) |
| Idempotencia | Diseñada de forma ligera; no implementada por completo |
| Historial | Un registro por cambio de estado |
| Versionado | Prefijo `/api/v1` |
