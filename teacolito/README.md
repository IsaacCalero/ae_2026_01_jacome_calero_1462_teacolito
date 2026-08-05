# Teacolito (microservicio)

API REST para gestionar gastos compartidos entre grupos de personas (estilo "Splitwise"): creación de grupos de gasto, miembros, gastos, cálculo de balances, netting de deudas (minimizar transferencias) y registro de pagos (settlements).

Este microservicio es parte del monorepo `TeAcolitoapp` (junto con `users/` y `nginx/`). Para levantar el sistema completo, ver el [README raíz](../README.md).

## Stack tecnológico

- **Kotlin** 2.2.0
- **Spring Boot** 4.0.0 (Web, Data JPA, OAuth2 Resource Server)
- **Java** 21 (toolchain)
- **Gradle** 8.14 (wrapper incluido)
- **PostgreSQL** (base de datos real, vía Docker Compose — ver `teacolito-db` en el `docker-compose.yml` raíz)
- **Flyway** para versionar el esquema (`src/main/resources/db/migration/`)
- **JWT / OAuth2** vía AWS Cognito como emisor de tokens (mismo issuer que `users`, ver README raíz)

## Estructura del proyecto

```
src/main/kotlin/com/pucetec/teacolito/
├── clients/          # Cliente HTTP hacia el microservicio `users` (resuelve displayName)
├── config/           # Configuración de seguridad (JWT / OAuth2)
├── controllers/       # Endpoints REST
├── dto/               # Request/Response DTOs
├── entities/          # Entidades JPA
├── exceptions/         # Excepciones de negocio + manejador global
├── logging/           # Filtro de logging HTTP (event=http.request/http.response, MDC sub)
├── mappers/           # Conversión entre entidades y DTOs
├── repositories/        # Repositorios Spring Data JPA
└── services/           # Lógica de negocio
```

## Cómo levantar SOLO este servicio (desarrollo local, sin Docker)

Requiere una instancia de PostgreSQL corriendo localmente (o usar el `teacolito-db` del compose raíz exponiendo el puerto temporalmente). Ajusta `SPRING_DATASOURCE_*` según tu entorno.

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

Para el flujo normal (todo el sistema junto, con Postgres, nginx y `users`), usa `docker compose up -d` desde la raíz del monorepo — ver el [README raíz](../README.md).

## Autenticación

La API está protegida con **JWT** validado contra AWS Cognito (issuer + `client_id`, ver `SecurityConfig.kt` y la sección Cognito del README raíz). Todas las peticiones a `/**` requieren un token JWT válido en el header `Authorization: Bearer <token>` — no hay endpoints públicos (la invitación por código también requiere JWT, ver Fase 2 del historial de decisiones).

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/expense-groups` | Crear un grupo de gastos |
| GET | `/expense-groups/{id}` | Obtener un grupo |
| PUT | `/expense-groups/{id}` | Actualizar un grupo |
| DELETE | `/expense-groups/{id}` | Eliminar un grupo (bloqueado si hay balances pendientes) |
| PATCH | `/expense-groups/{id}/close` | Cerrar un grupo |
| POST | `/groups/join` | Unirse a un grupo por código de invitación (username sale del JWT) |
| GET | `/expense-groups/{id}/members` | Listar miembros |
| GET | `/expense-groups/{id}/balances` | Balances por miembro |
| GET | `/expense-groups/{id}/netting` | Transferencias mínimas para saldar deudas |
| GET | `/invitations/{code}` | Consultar invitación (requiere JWT) |
| POST | `/expenses` | Registrar un gasto |
| GET | `/expense-groups/{id}/expenses` | Listar gastos de un grupo |
| PUT | `/expenses/{id}` | Actualizar un gasto |
| DELETE | `/expenses/{id}` | Eliminar un gasto |
| POST | `/settlements` | Registrar un pago/liquidación |
| GET | `/expense-groups/{id}/settlements` | Listar liquidaciones de un grupo |
| DELETE | `/settlements/{id}` | Eliminar una liquidación |

> Rutas mostradas sin el prefijo de nginx (`/teacolito/...`) — ver `nginx/nginx.conf` en la raíz para el ruteo real.

## Ejecutar pruebas

```bash
.\gradlew.bat test    # Windows
./gradlew test        # Linux / macOS
```

109 tests (unitarios + integración con Testcontainers Postgres). Detalle de cobertura y exclusiones en el [README raíz](../README.md#tests-y-cobertura-teacolito).

## Estado actual del backend

**Implementado:** CRUD completo de grupos, unión por código de invitación, gestión de miembros, gastos con reparto (`ExpenseShare`) validado contra el monto total, liquidaciones (`Settlement`), cálculo de balances netos, algoritmo de netting (mínimas transferencias), resolución de `displayName` vía el microservicio `users`, autenticación JWT/Cognito, manejo global de excepciones de negocio, logging estándar (formato de una línea, eventos de negocio, SQL logging), tests unitarios + integración.

**Pendiente:** ver la sección "Pendiente" del [README raíz](../README.md) para el estado global del proyecto (auditoría de negocio, documentación de Postman, etc.).
