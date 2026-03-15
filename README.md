# BTG Funds Backend

Backend reactivo para la gestion de suscripciones a fondos de inversion, desarrollado como prueba tecnica con enfoque en buenas practicas de arquitectura, seguridad y mantenibilidad.

## Tecnologias utilizadas

- Java 21
- Spring Boot 3.5 (WebFlux, Security)
- Maven
- AWS SDK v2 (DynamoDB Enhanced Async, SES)
- Twilio SDK
- JWT con `jjwt` 0.13
- OpenAPI/Swagger (`springdoc-openapi`)
- JUnit 5, Mockito, Reactor Test
- JaCoCo para cobertura
- Docker
- AWS (ECR, ECS Fargate, ALB, CloudWatch, CloudFormation)

## Arquitectura: Hexagonal (Ports & Adapters)

El proyecto implementa arquitectura hexagonal para separar claramente el dominio y los casos de uso de los detalles de infraestructura.

### Capas principales

- **Dominio (`domain`)**: modelos y reglas de negocio puras.
- **Aplicacion (`application`)**: casos de uso (`usecases`) y contratos (`ports`).
- **Infraestructura (`infrastructure`)**: adaptadores de entrada/salida, configuracion, seguridad y detalles tecnicos.

### Flujo en alto nivel

1. Un adaptador de entrada (REST/Router WebFlux) recibe la solicitud.
2. El adaptador invoca un **Puerto de Entrada** (`ports.in`).
3. El caso de uso (Interactor) ejecuta reglas de negocio.
4. El caso de uso usa **Puertos de Salida** (`ports.out`) para persistencia/notificaciones.
5. Adaptadores de salida resuelven esos puertos (DynamoDB, SES, Twilio).

## Patrones de diseño y decisiones de arquitectura

### 1) Ports and Adapters (Hexagonal)

- **Objetivo**: desacoplar negocio de frameworks y proveedores.
- **Donde se ve**:
  - Puertos de entrada: `application/ports/in/*`
  - Puertos de salida: `application/ports/out/*`
  - Adaptadores: `infrastructure/adapters/*` y `infrastructure/entrypoints/*`

### 2) Use Case / Interactor

- **Objetivo**: encapsular cada caso de negocio en una unidad clara.
- **Donde se ve**:
  - `SubscribeFundInteractor`
  - `UnsubscribeFundInteractor`
  - `GetCustomerFundsInteractor`

### 3) Repository Pattern (a traves de puertos)

- **Objetivo**: abstraer acceso a datos.
- **Donde se ve**:
  - Interfaces: `CustomerRepository`, `FundRepository`
  - Implementaciones DynamoDB: `DynamoDbCustomerAdapter`, `DynamoDbFundAdapter`

### 4) Adapter Pattern

- **Objetivo**: integrar proveedores externos sin contaminar el dominio.
- **Donde se ve**:
  - `NotificationAdapter` (SES/Twilio)
  - Adaptadores DynamoDB
  - Entry points WebFlux (handler/router)

### 5) Dependency Injection (IoC)

- **Objetivo**: invertir dependencias y facilitar pruebas.
- **Donde se ve**:
  - `UseCaseConfig` crea interactores inyectando puertos.
  - Spring resuelve implementaciones concretas en runtime.

### 6) Strategy simple por canal de notificacion

- **Objetivo**: seleccionar comportamiento por tipo de canal (`SMS` o `EMAIL`).
- **Donde se ve**:
  - `NotificationAdapter.send(...)` enruta a `sendSms` o `sendEmail`.

### 7) Fail-safe en integraciones externas

- **Objetivo**: no perder la transaccion principal por una falla de notificacion.
- **Donde se ve**:
  - En suscripcion, si falla el envio de notificacion se registra error y la transaccion persiste.

## Estructura de carpetas (resumen)

```text
src/main/java/com/btg/funds
  |- domain
  |- application
  |   |- ports
  |   |   |- in
  |   |   \- out
  |   \- usecases
  \- infrastructure
      |- adapters
      |- config
      |- entrypoints
      \- security
```

## Instalación y ejecución

## Requisitos

- JDK 21
- Maven 3.9+
- Docker (opcional para despliegue)
- Cuenta AWS (para entorno cloud)

### 1) Clonar e instalar dependencias

```bash
git clone https://github.com/KevinL0pez/invesment-btg-funds.git
cd funds
mvn clean compile
```

### 2) Configurar variables de entorno

El proyecto soporta carga desde `.env` con:

```yaml
spring.config.import: optional:file:.env[.properties]
```

Usa un archivo `.env` con valores de tu entorno (sin versionar secretos reales).

Variables importantes:

- `SPRING_PROFILES_ACTIVE` (`local` o `prod`)
- `AWS_REGION`
- `AWS_ENDPOINT`
- `AWS_SES_ENDPOINT`
- `DYNAMODB_CUSTOMERS_TABLE`
- `DYNAMODB_FUNDS_TABLE`
- `DYNAMODB_TRANSACTIONS_TABLE`
- `SES_MAIL`
- `TWILIO_SID`, `TWILIO_TOKEN`, `TWILIO_PHONE` o `TWILIO_MESSAGING_SERVICE_SID`
- `JWT_SECRET`, `JWT_EXPIRATION`
- `DEMO_USERNAME`, `DEMO_PASSWORD`

### 3) Ejecutar en local

```bash
mvn spring-boot:run
```

La API queda disponible en:

- `http://localhost:8080`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Coleccion de Postman

Se incluye la coleccion:

- `BtgFunds.postman_collection.json`

Pasos recomendados:

1. Importar la coleccion en Postman.
2. Crear una variable de coleccion `baseUrl`.
3. Configurar `baseUrl` segun entorno:
   - Local: `http://localhost:8080`
   - AWS: `http://<tu-alb>.us-east-1.elb.amazonaws.com`
4. Ejecutar primero el login para obtener JWT y reutilizarlo en endpoints protegidos.

## Pruebas unitarias

### Ejecutar tests

```bash
mvn test
```

### Cobertura con JaCoCo

- Reporte HTML: `target/site/jacoco/index.html`
- Verificacion de umbral minimo en fase `verify`:

```bash
mvn verify
```

> Nota: el `pom.xml` ya incluye configuracion de `jacoco-maven-plugin` con includes dirigidos a los paquetes cubiertos por pruebas.

## Seguridad y configuracion

- JWT firmado con clave configurable (`JWT_SECRET`) y expiracion (`JWT_EXPIRATION`).
- Credenciales AWS resueltas via `DefaultCredentialsProvider` (recomendado para ECS Task Role) o credenciales explicitas.
- Manejo de errores con respuestas HTTP consistentes (`400`, `404`, `500`) y trazabilidad en logs.

## Despliegue en AWS

La infraestructura se despliega con CloudFormation:

- Plantilla ECR: `cloudformation/ecr.yaml`
- Plantilla ECS/ALB: `cloudformation/backend-ecs.yaml`
- Guia paso a paso: `DEPLOY_CLOUDFORMATION.md`

Adicionalmente, para continuar con los demas procesos del proyecto puedes apoyarte en estas dos guias:

- `DEPLOY_CLOUDFORMATION.md`: flujo completo de build, push y despliegue en AWS.
- `GUIA_INFRA_AWS_TWILIO.md`: configuracion de DynamoDB, SES, Twilio e IAM.

## Estado del proyecto

El backend soporta:

- Autenticacion JWT
- Suscripcion y cancelacion de fondos
- Consulta de fondos por cliente
- Persistencia en DynamoDB
- Notificaciones por Email (SES) y SMS (Twilio)
- Despliegue productivo en ECS Fargate
