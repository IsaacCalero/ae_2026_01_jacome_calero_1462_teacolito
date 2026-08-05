# Teacolito

API REST para gestionar gastos compartidos entre grupos de personas (estilo "Splitwise"): creación de grupos de gasto, miembros, gastos, cálculo de balances, netting de deudas (minimizar transferencias) y registro de pagos (settlements).

## Stack tecnológico

- **Kotlin** 2.2.0
- **Spring Boot** 4.0.0 (Web, Data JPA, OAuth2 Resource Server)
- **Java** 21 (toolchain)
- **Gradle** 8.14 (wrapper incluido)
- **H2** (base de datos en memoria, para desarrollo/pruebas)
- **JWT / OAuth2** vía AWS Cognito como emisor de tokens

## Estructura del proyecto

```
src/main/kotlin/com/pucetec/teacolito/
├── config/          # Configuración de seguridad (JWT / OAuth2)
├── controllers/      # Endpoints REST
├── dto/              # Request/Response DTOs
├── entities/         # Entidades JPA
├── exceptions/        # Excepciones de negocio + manejador global
├── mappers/          # Conversión entre entidades y DTOs
├── repositories/       # Repositorios Spring Data JPA
└── services/          # Lógica de negocio
```

## Requisitos previos

- JDK 21 (el `build.gradle.kts` fija el toolchain en 21; IntelliJ o el propio wrapper lo pueden descargar si no lo tienes)
- No necesitas tener Gradle instalado: el proyecto incluye el wrapper (`gradlew` / `gradlew.bat`)

## Cómo levantar el proyecto

### Opción 1: Desde la terminal

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

### Opción 2: Desde IntelliJ IDEA

1. Abre el proyecto (IntelliJ detecta automáticamente el `build.gradle.kts`).
2. Espera a que sincronice las dependencias de Gradle.
3. Ejecuta la clase `TeacolitoApplication.kt` (ubicada en `src/main/kotlin/com/pucetec/teacolito/`) con el botón ▶ run.

La aplicación arranca en **http://localhost:8080**.

## Base de datos

Por defecto usa **H2 en memoria** (no requiere instalación ni configuración adicional). La consola web de H2 está habilitada en:

```
http://localhost:8080/h2-console
```

Datos de conexión (ver `src/main/resources/application.yml`):
- JDBC URL: `jdbc:h2:mem:teacolito`
- Usuario: `sa`
- Contraseña: *(vacía)*

Los datos se pierden al reiniciar la aplicación (`ddl-auto: update`, sin datos persistentes).

Para desplegar contra una base de datos real (por ejemplo PostgreSQL), reemplaza el bloque `datasource` en `application.yml` según las instrucciones comentadas en ese mismo archivo, y agrega la dependencia `runtimeOnly("org.postgresql:postgresql")` en `build.gradle.kts`.

## Autenticación

La API está protegida con **JWT** validado contra un emisor OAuth2 de AWS Cognito (ver `spring.security.oauth2.resourceserver.jwt.issuer-uri` en `application.yml`). Todas las peticiones a `/api/**` requieren un token JWT válido en el header `Authorization: Bearer <token>`, **excepto** las peticiones `GET` a `/api/invitations/**`, que son públicas.

El nombre de usuario se extrae del propio token (claims `username` o `cognito:username`, o `sub` como último recurso — ver `JwtUsernameExtractor.kt`), por lo que no existe un registro de usuarios local: la identidad siempre viene delegada de Cognito.

### ⚠️ Importante para hacer pruebas manuales (Postman, curl, etc.)

El `issuer-uri` que trae el proyecto por defecto apunta a un **User Pool de Cognito específico** (`us-east-1_yzwNALI2A`). Si no tienes acceso a ese pool no vas a poder generar tokens válidos para probar los endpoints protegidos. Para poder probar la API con tus propios tokens debes:

1. Reemplazar `spring.security.oauth2.resourceserver.jwt.issuer-uri` en `src/main/resources/application.yml` por el issuer de **tu propio** User Pool de Cognito (o el que te compartan para el curso), con el formato:
   ```
   https://cognito-idp.<region>.amazonaws.com/<user-pool-id>
   ```
2. Generar un JWT válido para ese pool (por ejemplo autenticando un usuario de prueba desde el App Client y usando el `id_token` o `access_token` resultante).
3. Enviar ese token en el header `Authorization: Bearer <token>` en tus peticiones.

Sin este cambio, cualquier request a los endpoints protegidos devolverá `401 Unauthorized` porque Spring Security no podrá validar la firma/emisor del token contra un pool al que no tienes acceso. Las pruebas unitarias (`./gradlew test`) **no** se ven afectadas por esto, ya que no levantan el contexto de seguridad ni hacen peticiones HTTP reales.

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/expense-groups` | Crear un grupo de gastos |
| GET | `/api/expense-groups/{id}` | Obtener un grupo |
| PUT | `/api/expense-groups/{id}` | Actualizar un grupo |
| DELETE | `/api/expense-groups/{id}` | Eliminar un grupo |
| PATCH | `/api/expense-groups/{id}/close` | Cerrar un grupo |
| POST | `/api/expense-groups/{id}/members` | Agregar miembro al grupo |
| GET | `/api/expense-groups/{id}/members` | Listar miembros |
| GET | `/api/expense-groups/{id}/balances` | Balances por miembro |
| GET | `/api/expense-groups/{id}/netting` | Transferencias mínimas para saldar deudas |
| GET | `/api/invitations/{code}` | Consultar invitación (público) |
| POST | `/api/expenses` | Registrar un gasto |
| GET | `/api/expense-groups/{id}/expenses` | Listar gastos de un grupo |
| PUT | `/api/expenses/{id}` | Actualizar un gasto |
| DELETE | `/api/expenses/{id}` | Eliminar un gasto |
| POST | `/api/settlements` | Registrar un pago/liquidación |
| GET | `/api/expense-groups/{id}/settlements` | Listar liquidaciones de un grupo |
| DELETE | `/api/settlements/{id}` | Eliminar una liquidación |

## Ejecutar pruebas

```bash
.\gradlew.bat test    # Windows
./gradlew test        # Linux / macOS
```

## Estado actual del backend

**Implementado:**
- CRUD completo de grupos de gasto (`ExpenseGroup`), con cierre de grupo (`closed`) para impedir nuevos gastos.
- Gestión de miembros por grupo, con validación de duplicados y de pertenencia al grupo.
- Registro de gastos (`Expense`) con reparto entre miembros (`ExpenseShare`), validando que la suma de los repartos coincida exactamente con el monto del gasto.
- Registro de pagos/liquidaciones (`Settlement`) entre miembros.
- Cálculo de balances netos por miembro (`/balances`) a partir de gastos, repartos y liquidaciones.
- Algoritmo de **netting** (`/netting`): reduce las deudas cruzadas del grupo al mínimo número de transferencias necesarias (algoritmo greedy tipo "min cash flow").
- Invitaciones a grupo por código público (`GET /api/invitations/{code}`), sin requerir autenticación.
- Autenticación **stateless** vía JWT (AWS Cognito como resource server), con autorización a nivel de servicio (solo el creador puede editar/cerrar/eliminar un grupo, solo el dueño de un gasto o pago puede editarlo/eliminarlo, solo miembros del grupo pueden consultar su información).
- Manejo global de excepciones de negocio (`GlobalExceptionHandler`) que traduce cada excepción a un código HTTP semántico (400, 403, 404, 409) con un cuerpo de error consistente.
- Pruebas unitarias (JUnit 5 + Mockito) de los tres servicios (`ExpenseGroupService`, `ExpenseService`, `SettlementService`), mockeando los repositorios.

**Pendiente / fuera de alcance por ahora:**
- No hay un endpoint de "unirme al grupo" usando el código de invitación (`GET /api/invitations/{code}` solo consulta datos; agregar miembros se hace vía `POST /api/expense-groups/{id}/members` con el username).
- No hay pruebas de integración/HTTP (`@SpringBootTest` + `MockMvc`), solo pruebas unitarias de servicios.
- No hay base de datos persistente configurada por defecto (usa H2 en memoria); la migración a PostgreSQL está documentada pero no aplicada.
- No hay documentación interactiva de la API (Swagger/OpenAPI).
- No hay paginación en los listados (miembros, gastos, liquidaciones).
- No hay pipeline de CI/CD.

## Cómo explicarlo ante un profesor

Sugerencia de guion para sustentar el proyecto:

1. **Problema que resuelve**: una API para dividir gastos compartidos entre grupos de personas (viajes, roomies, eventos), similar a Splitwise — permite registrar quién pagó qué, cuánto le debe cada quien al grupo, y minimizar cuántas transferencias hacen falta para saldar las deudas.
2. **Arquitectura**: capas clásicas de Spring Boot — `controllers` (HTTP) → `services` (reglas de negocio y autorización) → `repositories` (Spring Data JPA) → `entities` (modelo de datos). Los `dto` y `mappers` desacoplan lo que se expone por HTTP de las entidades persistidas, y `exceptions` centraliza los errores de negocio en un único manejador global para respuestas consistentes.
3. **Lo más interesante para destacar**:
   - El cálculo de **balances** (`computeBalances` en `ExpenseGroupService`): suma lo que cada quien pagó, resta lo que le corresponde de cada gasto (`ExpenseShare`) y ajusta con las liquidaciones ya realizadas.
   - El **algoritmo de netting**: en vez de mostrar todas las deudas cruzadas, calcula el mínimo de transferencias entre deudores y acreedores emparejando siempre el mayor deudor con el mayor acreedor.
   - La **seguridad**: no hay login propio, la identidad viene de un JWT emitido por AWS Cognito (arquitectura *resource server*); la autorización fina (por ejemplo, "solo el creador puede cerrar el grupo") se valida a mano en los servicios, no solo con anotaciones de Spring Security.
4. **Cómo se prueba**: mostrar `./gradlew test` corriendo las pruebas unitarias, y opcionalmente una demo en vivo con Postman contra `http://localhost:8080` usando un token JWT de un pool de Cognito propio (ver sección de autenticación arriba).
5. **Si preguntan qué falta**: ser honesto y mencionar la sección "Pendiente" de arriba — pruebas de integración end-to-end, flujo de unión a grupo por invitación, y despliegue con base de datos persistente en la nube.
