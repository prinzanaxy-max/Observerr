# Observerr Backend

## Overview

Spring Boot 4.1.0 backend for Observerr — an online exam integrity monitoring platform. Provides a stateless JWT-based authentication system with role-based access control (RBAC) for Students, Lecturers, and Administrators.

## Live Deployment

| Environment | URL |
|---|---|
| Production (Railway) | `https://observerr-production.up.railway.app` |
| Local dev | `http://localhost:8080` |

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security 6 + JWT (jjwt 0.12.3) |
| Database | PostgreSQL (Neon serverless) |
| ORM | Spring Data JPA / Hibernate 7 |
| Build | Maven |
| Utilities | Lombok |

---

## Getting Started (Local)

### Prerequisites

- Java 21+
- Maven 3.9+
- A Neon PostgreSQL database (or any PostgreSQL 14+ instance)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/prinzanaxy-max/Observerr.git
   cd observerr
   ```

2. Set the required environment variables (or export them in your shell):
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:postgresql://<host>/<db>?sslmode=require
   export SPRING_DATASOURCE_USERNAME=<username>
   export SPRING_DATASOURCE_PASSWORD=<password>
   export JWT_SECRET=<your-secret-min-32-chars>
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

4. API available at: `http://localhost:8080`

---

## Environment Variables

All configuration is injected via environment variables. Set these in Railway (Settings → Variables) or your local shell.

| Variable | Required | Default | Description |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Yes | — | Neon PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Yes | — | DB username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | — | DB password |
| `JWT_SECRET` | Yes | — | JWT signing secret (min 32 chars / 256 bits) |
| `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | No | `org.postgresql.Driver` | JDBC driver |
| `SPRING_JPA_DATABASE_PLATFORM` | No | `PostgreSQLDialect` | Hibernate dialect |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | No | `update` | Schema strategy |
| `SPRING_JPA_SHOW_SQL` | No | `false` | Log SQL queries |
| `SERVER_PORT` | No | `8080` | HTTP port |
| `JWT_EXPIRATION` | No | `86400000` | Access token TTL (ms) — 24h |
| `JWT_REFRESH_EXPIRATION` | No | `604800000` | Refresh token TTL (ms) — 7 days |
| `JWT_ISSUER` | No | `observerr` | JWT issuer claim |
| `SPRING_WEB_CORS_ALLOWED_ORIGINS` | No | `http://localhost:5178` | Allowed CORS origin |
| `SPRING_PROFILES_ACTIVE` | No | — | Set to `production` on Railway |

---

## Authentication

All protected routes require:
```
Authorization: Bearer <access_token>
```

Tokens are obtained from `/api/auth/login` or `/api/auth/register`.

---

## API Endpoints

### Health

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/health` | No | Server health check |

### Auth

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register a new user |
| `POST` | `/api/auth/login` | No | Login and receive tokens |
| `POST` | `/api/auth/refresh` | Refresh token | Get a new access token |
| `GET` | `/api/auth/me` | Access token | Get current user info |

### Student (requires `STUDENT` role)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/student/hello` | Student role test endpoint |

### Lecturer (requires `LECTURER` role)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/lecturer/hello` | Lecturer role test endpoint |

---

## Request / Response Examples

### Health Check

```bash
curl https://observerr-production.up.railway.app/health
```
```json
{
  "status": "UP",
  "app": "Observerr Backend",
  "timestamp": "2026-07-02T09:00:00",
  "version": "1.0.0"
}
```

### Register

```bash
curl -X POST https://observerr-production.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Dr. Kwame Mensah",
    "email": "kwame@university.edu",
    "password": "password123",
    "role": "LECTURER"
  }'
```

Response `201 Created`:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "role": "LECTURER",
  "fullName": "Dr. Kwame Mensah",
  "expiresIn": 86400000
}
```

### Login

```bash
curl -X POST https://observerr-production.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "kwame@university.edu", "password": "password123"}'
```

Response `200 OK`: same shape as register.

### Get Current User

```bash
curl https://observerr-production.up.railway.app/api/auth/me \
  -H "Authorization: Bearer <access_token>"
```
```json
{
  "id": 1,
  "email": "kwame@university.edu",
  "fullName": "Dr. Kwame Mensah",
  "role": "LECTURER",
  "createdAt": "2026-07-02T09:00:00"
}
```

### Refresh Token

```bash
curl -X POST https://observerr-production.up.railway.app/api/auth/refresh \
  -H "Authorization: Bearer <refresh_token>"
```

### Error Responses

All errors follow this shape:
```json
{
  "error": "ERROR_CODE",
  "message": "Human readable message",
  "timestamp": "2026-07-02T10:00:00"
}
```

Validation errors (400) include a field-level `errors` map:
```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "errors": {
    "email": "Must be a valid email address",
    "password": "Password must be at least 8 characters"
  },
  "timestamp": "2026-07-02T10:00:00"
}
```

---

## HTTP Status Codes

| Code | Scenario |
|---|---|
| `200` | Successful login / refresh / info |
| `201` | Successful registration |
| `400` | Validation errors |
| `401` | Missing / invalid / expired token, wrong credentials |
| `403` | Valid token but insufficient role |
| `409` | Email already registered |
| `500` | Unexpected server error |

---

## Project Structure

```
com.backend.observerr/
├── config/
│   ├── SecurityConfig.java          — filter chain, RBAC rules, beans
│   ├── CorsConfig.java              — CORS for localhost:* (dev) and production
│   └── HealthController.java        — GET /health
├── auth/
│   ├── controller/AuthController.java
│   ├── service/
│   │   ├── AuthService.java         — register, login, refreshToken
│   │   ├── JwtService.java          — token generation & validation
│   │   └── CustomUserDetailsService.java
│   ├── filter/JwtAuthenticationFilter.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   └── AuthResponse.java
│   └── model/
│       ├── User.java                — JPA entity + UserDetails
│       ├── Role.java                — STUDENT / LECTURER / ADMIN
│       └── UserRepository.java
├── exception/GlobalExceptionHandler.java
├── student/StudentController.java   — GET /api/student/hello
└── lecturer/LecturerController.java — GET /api/lecturer/hello
```

---

## Security Design

- Passwords hashed with BCrypt (cost factor 12) — never stored or returned plain
- JWT access tokens expire in 24h; refresh tokens expire in 7 days
- Same error message for wrong email and wrong password (`"Invalid credentials"`) — prevents user enumeration
- Stateless sessions — no `HttpSession` anywhere
- CSRF disabled (stateless JWT APIs do not need it)
- CORS uses `allowedOriginPatterns("http://localhost:*")` for local dev; set `SPRING_WEB_CORS_ALLOWED_ORIGINS` to your production frontend URL

---

## Postman Collection

Import `postman/Observerr_Auth.postman_collection.json` into Postman.

Set up a Postman environment with `baseUrl = https://observerr-production.up.railway.app` (or `http://localhost:8080` for local). Run **Register** or **Login** first — the test scripts auto-save `accessToken` and `refreshToken` to your environment.
