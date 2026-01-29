# EventHub Cloud

Sistema de eventos asíncronos con Spring Boot, PostgreSQL y React. Implementa un patrón event-driven con procesamiento en segundo plano, pensado para demostrar arquitectura orientada a eventos en un contexto realista.

## ¿Qué hace?

Permite crear eventos que se guardan en BD y se procesan de forma asíncrona. El usuario crea un evento vía API, este se persiste con estado `PENDING`, se publica en una cola (Kafka o in-memory según el modo), y un consumer lo procesa cambiando su estado a `PROCESSING` y luego a `PROCESSED` o `FAILED`.

El frontend React se conecta mediante **Server-Sent Events (SSE)** para recibir actualizaciones en tiempo real, eliminando la necesidad de recargar la página.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           EVENTHUB CLOUD                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐         ┌─────────────────────────────────────────┐   │
│  │             │         │           SPRING BOOT API               │   │
│  │   REACT     │  HTTP   │  ┌─────────┐  ┌─────────┐  ┌─────────┐  │   │
│  │   FRONTEND  │────────▶│  │Controller│──│ Service │──│   JPA   │  │   │
│  │   (Vite)    │         │  └─────────┘  └────┬────┘  └────┬────┘  │   │
│  │             │         │                    │            │        │   │
│  └─────────────┘         │              ┌─────▼─────┐      │        │   │
│                          │              │  Event    │      │        │   │
│                          │              │ Publisher │      │        │   │
│                          │              └─────┬─────┘      │        │   │
│                          └────────────────────┼────────────┼────────┘   │
│                                               │            │            │
│              ┌────────────────────────────────┼────────────┼──────┐     │
│              │                                ▼            ▼      │     │
│              │  ┌─────────────────┐    ┌───────────────────────┐ │     │
│              │  │                 │    │                       │ │     │
│              │  │  KAFKA/REDPANDA │    │     POSTGRESQL        │ │     │
│              │  │  (or In-Memory) │    │                       │ │     │
│              │  │                 │    │                       │ │     │
│              │  └────────┬────────┘    └───────────────────────┘ │     │
│              │           │                                       │     │
│              │           ▼                                       │     │
│              │  ┌─────────────────┐                              │     │
│              │  │  EVENT CONSUMER │──────────────────────────────┘     │
│              │  │  (Updates DB)   │                                    │
│              │  └─────────────────┘                                    │
│              │       INFRASTRUCTURE                                    │
│              └─────────────────────────────────────────────────────────┘
│                                                                         │
└────────────────────────────────────SSE (Real-time)──────────────────────┘

Flujo:
1. Usuario crea evento via React UI
2. API valida y guarda en PostgreSQL (status: PENDING)
3. Evento se publica al topic "events"
4. Consumer recibe mensaje, lo procesa
5. Consumer actualiza estado a PROCESSED
6. Frontend recibe actualización en tiempo real vía SSE (sin recargar)
```

## Modos de ejecución

El proyecto soporta dos modos:

### Modo local (con Kafka)
- Usa Kafka/Redpanda real para la cola de mensajes
- Requiere Docker con Kafka corriendo
- Activar con: `SPRING_PROFILES_ACTIVE=local`

### Modo demo (sin Kafka)
- Cola en memoria simulada
- Perfecto para desplegar en plataformas gratuitas (Render, Railway, Fly.io) que no permiten Kafka
- El procesamiento asíncrono funciona igual, solo que sin dependencia externa
- Activar con: `SPRING_PROFILES_ACTIVE=demo` o `KAFKA_ENABLED=false`

En producción real usaría Kafka, pero para una demo o entorno de desarrollo sin recursos, el modo in-memory sirve igual para mostrar el concepto.

## Ejecutar en local

### Requisitos
- Java 17+
- Docker + Docker Compose
- Node.js 18+ (para el frontend)
- Maven 3.8+

### Opción 1: Todo con Docker

```bash
git clone https://github.com/Hacktreyu/eventhub-cloud-production.git
cd eventhub-cloud

# Levantar todo (Postgres, Kafka, API, Frontend)
docker-compose up --build

# URLs:
# Frontend: http://localhost:3000
# API:      http://localhost:8080
# Swagger:  http://localhost:8080/swagger-ui.html
# Kafka UI: http://localhost:8090
```

### Opción 2: API fuera de Docker (desarrollo)

```bash
# Levantar solo infraestructura
docker-compose -f infra/docker-compose.dev.yml up -d

# Ejecutar API
cd api
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Ejecutar frontend
cd ../app
npm install
npm run dev
```

### Opción 3: Modo demo (sin Kafka)

```bash
cd api
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

## Estructura del proyecto

```
eventhub-cloud/
├── api/                          # Backend Spring Boot
│   ├── src/main/java/com/eventhub/api/
│   │   ├── config/               # Config de OpenAPI, Kafka, etc.
│   │   ├── consumer/             # Consumers (Kafka e In-Memory)
│   │   ├── controller/           # REST endpoints
│   │   ├── dto/                  # Request/Response DTOs
│   │   ├── entity/               # Entidades JPA
│   │   ├── exception/            # Handlers de excepciones
│   │   ├── repository/           # Repositorios
│   │   ├── service/              # Lógica de negocio
│   │   └── EventHubApplication.java
│   ├── src/main/resources/
│   │   └── application.yml       # Config con perfiles
│   ├── src/test/java/            # Tests unitarios e integración
│   ├── Dockerfile
│   └── pom.xml
│
├── app/                          # Frontend React
│   ├── src/
│   │   ├── App.jsx
│   │   ├── index.css
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
│
├── infra/
│   └── docker-compose.dev.yml    # Solo deps (Postgres, Kafka)
│
├── .github/workflows/
│   └── ci.yml                    # CI/CD con GitHub Actions
│
├── docker-compose.yml            # Stack completo
└── README.md
```

## API - Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/events` | Crear evento |
| `GET` | `/api/events` | Listar todos |
| `GET` | `/api/events/{id}` | Ver por ID |
| `GET` | `/api/events/status/{status}` | Filtrar por estado |
| `GET` | `/api/events/stats` | Estadísticas |
| `GET` | `/actuator/health` | Health check |

### Ejemplo de uso

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "title": "User Signed Up",
    "description": "New user registration event",
    "source": "auth-service",
    "type": "USER_ACTION"
  }'
```

Respuesta:
```json
{
  "id": 1,
  "title": "User Signed Up",
  "description": "New user registration event",
  "source": "auth-service",
  "type": "USER_ACTION",
  "status": "PENDING",
  "createdAt": "2025-01-27T22:30:00",
  "processedAt": null,
  "retryCount": 0
}
```

## Deploy gratuito

### Backend en Render

1. Crear cuenta en [render.com](https://render.com)
2. New Web Service desde el repo de GitHub
3. Config:
   - **Root Directory**: `api`
   - **Build**: `./mvnw clean package -DskipTests`
   - **Start**: `java -jar target/*.jar`
   - **Variables**:
     ```
     SPRING_PROFILES_ACTIVE=demo
     KAFKA_ENABLED=false
     DATABASE_URL=jdbc:postgresql://...
     DATABASE_USER=...
     DATABASE_PASSWORD=...
     ```
4. Añadir PostgreSQL (free tier)

### Frontend en Vercel

1. Importar repo en [vercel.com](https://vercel.com)
2. Config:
   - **Root Directory**: `app`
   - **Framework**: Vite
   - **Vars**: `VITE_API_URL=https://tu-api.onrender.com`
3. Deploy

## Tests

```bash
cd api

# Todos los tests
./mvnw test

# Solo unitarios
./mvnw test -Dtest=!*IntegrationTest

# Solo integración (necesita Docker para Testcontainers)
./mvnw test -Dtest=*IntegrationTest

# Cobertura
./mvnw verify jacoco:report
# Ver en: api/target/site/jacoco/index.html
```

## Variables de entorno

### Backend

| Variable | Default | Uso |
|----------|---------|-----|
| `PORT` | `8080` | Puerto del servidor |
| `SPRING_PROFILES_ACTIVE` | `local` | `local` o `demo` |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/eventhub` | URL de PostgreSQL |
| `DATABASE_USER` | `eventhub` | Usuario BD |
| `DATABASE_PASSWORD` | `eventhub123` | Password BD |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka servers |
| `KAFKA_ENABLED` | `true` | Activar/desactivar Kafka |

### Frontend

| Variable | Default | Uso |
|----------|---------|-----|
| `VITE_API_URL` | `""` (vacío, se pone manual) | URL del backend |

## Stack técnico

**Backend**:
- Java 17
- Spring Boot 3.2.1 (Web, Data JPA, Kafka)
- PostgreSQL
- Springdoc OpenAPI
- Lombok
- JUnit 5 + Mockito
- Testcontainers

**Frontend**:
- React 18
- Vite 5
- CSS vanilla

**Infraestructura**:
- Docker + Docker Compose
- Redpanda (compatible Kafka)
- GitHub Actions

## Licencia

MIT - Usar libremente como template.
