# Events Gateway

A Spring Boot API gateway service that routes requests to multiple microservices including user management, authentication, email, and event management services.

## Project Description

Events Gateway is a reactive gateway built with Spring WebFlux that acts as a reverse proxy, forwarding HTTP requests to different backend microservices based on the request path. It provides:

- Request forwarding to multiple backend services
- Request/response logging
- Centralized entry point for the events system

## Prerequisites

- Java 21
- Gradle 8.14.3 (or use the included Gradle wrapper)
- Docker (optional, for containerized deployment)

## Building the Project

### Using Gradle

Build the project without running tests:

```bash
./gradlew clean build
```

Or on Windows:

```bash
gradlew.bat clean build
```

Run the application locally:

```bash
./gradlew bootRun
```

## Docker

### Building the Docker Image

```bash
docker build -t events-gateway:latest .
```

### Running the Container

```bash
docker run -p 8080:8080 \
  -e USER_SERVICE_URL=http://events-user-service:8081/usuarios \
  -e AUTH_SERVICE_URL=http://events-user-service:8081/auth \
  -e EMAIL_SERVICE_URL=http://events-email-service:8082 \
  -e EVENT_SERVICE_URL=http://events-manager-service:8000 \
  events-gateway:latest
```

Or with Docker Compose (if part of a larger stack):

```bash
docker-compose up
```

## Configuration

### Environment Variables

Configure the backend service URLs using these environment variables or in `src/main/resources/application.properties`:

| Variable | Default | Description |
|----------|---------|-------------|
| `USER_SERVICE_URL` | `http://events-user-service:8081/usuarios` | User service base URL |
| `AUTH_SERVICE_URL` | `http://events-user-service:8081/auth` | Authentication service URL |
| `EMAIL_SERVICE_URL` | `http://events-email-service:8082` | Email service URL |
| `EVENT_SERVICE_URL` | `http://events-manager-service:8000` | Event manager service URL |
| `SERVER_PORT` | `8080` | Gateway server port |

## API Usage

The gateway routes requests to backend services based on the request path:

### User Service Routes
```bash
# Get users
curl http://localhost:8080/usuarios/listar

# Create user
curl -X POST http://localhost:8080/usuarios/criar \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@example.com"}'
```

### Authentication Routes
```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'
```

### Event Service Routes
```bash
# Get events
curl http://localhost:8080/eventos/listar

# Create event
curl -X POST http://localhost:8080/eventos/criar \
  -H "Content-Type: application/json" \
  -d '{"name":"Event Name","date":"2024-12-31"}'
```

### Email Service Routes
```bash
# Send email
curl -X POST http://localhost:8080/send-email \
  -H "Content-Type: application/json" \
  -d '{"to":"user@example.com","subject":"Test","body":"Hello"}'
```

## Project Structure

```
src/
├── main/
│   ├── java/com/eventos/eventsgateway/
│   │   ├── EventsGatewayApplication.java    # Main application
│   │   ├── config/
│   │   │   └── WebClientConfig.java         # WebClient configuration
│   │   ├── controller/
│   │   │   └── ProxyController.java         # Request routing controller
│   │   ├── filter/
│   │   │   └── LoggingFilter.java           # Request/response logging
│   │   └── service/
│   │       └── ProxyService.java            # Proxy forwarding logic
│   └── resources/
│       └── application.properties           # Configuration file
└── test/
    └── java/com/eventos/eventsgateway/
        └── EventsGatewayApplicationTests.java
```