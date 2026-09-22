PRD: Sistema de Gestión de Pedidos — API Gateway + Microservicios

Contexto: Este PRD formaliza un reto técnico backend que consiste en construir una plataforma de pedidos con arquitectura de microservicios, expuesta a través de un API Gateway con autenticación OAuth 2.0.

1. Resumen Ejecutivo
   Nombre del producto: Order Management Platform (OMP)
   Problema que resuelve: Las empresas necesitan un backend confiable para registrar pedidos de productos, validar disponibilidad de stock en tiempo real y dar trazabilidad completa a cada solicitud, sin exponer la lógica interna de los microservicios al cliente.
   Solución propuesta: Una arquitectura de microservicios (Order Service + Inventory Service) detrás de un API Gateway único, con autenticación OAuth 2.0/OIDC vía JWT, trazabilidad end-to-end mediante X-Trace-Id, y comunicación reactiva entre servicios.
   Propuesta de valor única: Backend desacoplado, seguro y trazable, 100% verificable vía Postman, que separa responsabilidades de negocio (microservicios) de responsabilidades de enrutamiento/seguridad (gateway).
2. Usuarios Objetivo
   Segmento primario: Equipos de desarrollo/QA que consumen la API vía Postman (clientes API técnicos), en el contexto de una evaluación técnica de backend.
   Segmento secundario: Sistemas cliente (apps móviles o web de terceros) que en un futuro consumirían el Gateway como punto único de entrada.
   Casos de uso principales:
   Registrar un nuevo pedido de producto.
   Consultar el estado actual de un pedido.
   Consultar el historial de cambios de estado de un pedido.
   Cancelar un pedido cuando su estado lo permita.
3. Objetivos y Métricas de Éxito
   Objetivos de negocio: Demostrar una arquitectura de microservicios funcional, segura y trazable que pueda evolucionar a un sistema de pedidos real.
   KPIs principales: 100% de endpoints documentados y probables desde Postman; 100% de solicitudes con X-Trace-Id propagado; 0 transiciones de estado inválidas permitidas.
   Criterios de éxito del MVP: La solución levanta con docker compose up --build, todos los flujos (crear, consultar, cancelar, historial) funcionan de extremo a extremo con autenticación válida.
4. Alcance del MVP

Incluido en v1:

API Gateway (Spring Cloud Gateway) como único punto público.
Order Service: creación, consulta de estado, historial y cancelación de pedidos.
Inventory Service: gestión de productos, stock y disponibilidad.
Autenticación OAuth 2.0/OIDC con JWT (Keycloak opcional).
Propagación de X-Trace-Id desde el Gateway hasta cada microservicio.
Persistencia de pedidos, historial, inventario y estados.
Documentación OpenAPI/Swagger + colección Postman.
Dockerización completa (Gateway, microservicios, BD, auth server).

Excluido explícitamente:

Frontend o BFF.
Saga Orchestrator, compensaciones distribuidas, Dispatch Service.
Kafka, Outbox pattern, Event Sourcing, CQRS.
Kubernetes (solo Docker/Docker Compose).

Criterios de lanzamiento: Todos los endpoints core operativos, autenticación funcional, trazabilidad verificable en logs, documentación completa.

5. Requerimientos Funcionales
   Registrar pedido: el usuario envía los datos del pedido; el sistema valida disponibilidad de stock antes de confirmar.
   Consultar estado: el usuario puede ver el estado actual de un pedido existente.
   Consultar historial: el usuario puede ver todos los cambios de estado que tuvo un pedido.
   Cancelar pedido: el usuario puede cancelar un pedido únicamente si su estado lo permite; no se permiten cancelaciones duplicadas.
   Flujo principal: Crear pedido → Validar stock en Inventory Service → Si hay stock, confirmar (estado CONFIRMED); si no, responder STOCK_INSUFFICIENT sin confirmar.
6. Requerimientos No Funcionales
   Rendimiento esperado: Comunicación síncrona reactiva (WebClient/WebFlux) entre servicios, sin bloqueos.
   Disponibilidad: Cada microservicio gestiona su propia persistencia de forma independiente, evitando puntos únicos de falla en la capa de datos.
   Seguridad y privacidad: Autenticación OAuth 2.0/OIDC con JWT en todas las rutas; el Gateway no debe contener lógica de negocio ni componer respuestas (no debe actuar como BFF).
   Trazabilidad: Todo request debe tener un X-Trace-Id generado o recibido en el Gateway, propagado a los microservicios e incluido en logs (Logback) y respuestas de error.
   Escala: Arquitectura preparada para agregar microservicios adicionales sin modificar el Gateway más allá de reglas de enrutamiento.
7. Modelo de Negocio
   Modelo de monetización: No aplica — es un reto técnico/prueba de concepto, no un producto comercial.
   Estrategia de go-to-market: No aplica en esta fase; el "usuario" es el evaluador técnico que prueba la API vía Postman.
   Mercado geográfico objetivo: No aplica (entorno de evaluación técnica).
8. Suposiciones y Riesgos
   Suposiciones clave: Se asume que "stock" es un valor simple por producto (sin variantes ni reservas temporales); se asume un solo tipo de "producto" sin categorías complejas.
   Riesgos identificados: Inconsistencia de datos entre Order Service e Inventory Service si la llamada síncrona falla a mitad de camino (no hay Saga); posible necesidad futura de idempotencia en creación de pedidos.
   Dependencias externas: Servidor de autenticación (Keycloak u equivalente) para emisión y validación de JWT.
9. Criterios de Aceptación
   Definición de "done": Los 4 flujos (crear, consultar estado, consultar historial, cancelar) funcionan de extremo a extremo, con autenticación obligatoria, trazabilidad visible en logs y respuestas, y errores en el formato { timestamp, status, code, message, traceId }.
   Escenarios de prueba principales:
   Crear pedido con stock disponible → estado CONFIRMED.
   Crear pedido sin stock → error STOCK_INSUFFICIENT, pedido no confirmado.
   Cancelar pedido en estado válido → éxito.
   Intentar cancelar un pedido ya cancelado → error de transición inválida.
   Request sin token o con token expirado → error 401 con traceId.
   Verificar que el mismo X-Trace-Id aparece en logs del Gateway y de ambos microservicios.
