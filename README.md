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
