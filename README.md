# Teacolito — Proyecto Integrador de Arquitectura Empresarial

> README raíz en construcción. Se completa en la Fase 7 con: integrantes, NRC, diagrama de
> arquitectura, pasos de arranque, estándar de logging y configuración de Cognito.

## Estructura del monorepo

```
├── users/          # Microservicio de usuarios (en construcción)
├── teacolito/       # Microservicio de gastos compartidos (ver teacolito/README.md)
├── nginx/          # API Gateway — único punto de entrada
├── docker-compose.yml
└── .env.example
```

## Cómo levantar el proyecto

1. Copiar `.env.example` a `.env` y completar los valores de Cognito.
2. `docker compose up -d`
3. La API queda expuesta a través de nginx en `http://localhost:9090`.

## Configuración de Cognito

- **Región**: `us-east-1`
- **User Pool ID**: `us-east-1_mNoOf1LKH`
- **App Client**: `teacolito`
- **Tipo de token validado**: Access Token (el `client_id` viaja en el claim `client_id`, no en `aud` — Cognito no pone el App Client en `aud` para Access Tokens, por eso `SecurityConfig.kt` valida `client_id` con un `OAuth2TokenValidator` propio en vez del validador de audience por defecto de Spring).
- **Grupos / roles**: no hay grupos de Cognito creados. El dominio no tiene roles globales — la autorización es contextual por recurso (ej. solo el creador de un grupo puede cerrarlo o eliminarlo, solo el dueño de un gasto puede editarlo), validada a mano en los services, no vía `@PreAuthorize` por rol. Decisión confirmada con el profesor.
- Los valores reales viven en `.env` (no versionado); `.env.example` documenta las claves sin secretos.

## Tests y cobertura (teacolito)

- **Unitarios** (JUnit 5 + Mockito): los 3 services (`ExpenseGroupService`, `ExpenseService`, `SettlementService`) y `CognitoClientIdValidator`.
- **Integración** (`@SpringBootTest` + `MockMvc` + Testcontainers Postgres): los 3 controllers, cubriendo caminos felices, errores de negocio (400/404/409) y autorización (401 sin token, 403 por no ser miembro/dueño del recurso). Incluye join por código, invitación privada, bloqueo de `DELETE` con balance pendiente, `ShareMismatchException`, y netting de extremo a extremo.
- **Excluido del objetivo de cobertura** (sin lógica propia que probar):
  - `TeacolitoApplication.kt` — clase de arranque, sin comportamiento.
  - `dto/*` — solo `data class` sin lógica.
  - `entities/*` — entidades JPA sin comportamiento más allá de sus campos.
  - `exceptions/*Exception.kt` — clases de una línea que solo extienden `RuntimeException`.
  - `SecurityConfig.kt` — configuración declarativa de Spring Security (DSL de beans), sin lógica propia; la única lógica real (`CognitoClientIdValidator`) sí tiene test unitario.
- Se corre con `./gradlew test` dentro de `teacolito/` (109 tests). Cobertura medida con el coverage del IDE (IntelliJ *Run with Coverage*, sobre `com.pucetec.teacolito`): **97% líneas, 97% métodos, 94% clases, 81% ramas**.
- **Huecos conocidos y por qué**:
  - `clients/UserClient` — solo se cubre la rama de fallback (falla la llamada → usa el username crudo). La rama de éxito (`users` responde bien) no se puede probar hasta que el microservicio `users` exista de verdad.
  - `config/SecurityConfig.jwtDecoder()` — el bean real (que llama a Cognito por red al arrancar) está anotado `@Profile("!test")` a propósito y nunca se instancia en los tests; solo se prueba la lógica propia (`CognitoClientIdValidator`), no la llamada de red en sí.

## Colección de Postman (teacolito)

- Archivos: [`teacolito/teacolito.postman_collection.json`](teacolito/teacolito.postman_collection.json) + [`teacolito/teacolito.postman_environment.json`](teacolito/teacolito.postman_environment.json), versionados en el repo.
- `base_url` apunta a nginx (`http://localhost:9090`), no a un puerto interno — todas las rutas usan el prefijo `/teacolito/`.
- 4 carpetas en orden de ejecución: **Auth** (obtiene el Access Token real de Cognito vía `InitiateAuth` con `USER_PASSWORD_AUTH` y lo guarda solo en variables de entorno, nunca hardcodeado) → **Expense Groups** → **Expenses** → **Settlements** → **Cleanup & group lifecycle**. 36 requests, cada uno con `pm.test`.
- Cubre join por código, invitación privada, `ShareMismatchException`, bloqueo de `DELETE` con balance pendiente, y casos explícitos de 400/404/409/401/403 — incluyendo 403 real usando dos usuarios de Cognito distintos (`user_a`/`user_b`).
- **Prerrequisito para correrla completa**: dos usuarios reales del User Pool con `ALLOW_USER_PASSWORD_AUTH` habilitado en el App Client `teacolito`, configurados en el environment (`user_a_username`/`user_a_password`/`user_b_username`/`user_b_password`) — no vienen incluidos por seguridad.
- **Validado con Newman** contra el stack real (`docker compose up`): las 36 requests llegan correctamente (0 fallas de red/ruteo); sin credenciales reales de Cognito, `Auth` devuelve 400 y todo lo que depende del token cae en cascada a 401 — es exactamente el comportamiento esperado sin login válido. Con usuarios de prueba reales debería correr en verde de punta a punta.
