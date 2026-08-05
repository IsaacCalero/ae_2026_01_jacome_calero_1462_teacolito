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
