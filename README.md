# Observerr Backend

## Overview

Spring Boot 4.1.0 backend for Observerr — an online exam integrity monitoring platform. Provides a stateless JWT-based authentication system with role-based access control (RBAC) for Students, Lecturers, and Administrators.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security 6 + JWT (jjwt 0.12.3) |
| Database | PostgreSQL (Neon serverless) |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Utilities | Lombok |

## Getting Started

### Prerequisites

- Java 25+
- Maven 3.9+
- A Neon PostgreSQL database (or any PostgreSQL 14+ instance)

### Setup

1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd observerr
   ```

2. Configure `src/main/resources/application.properties` with your database credentials and JWT secret:
   ```properties
   spring.datasource.url=jdbc:postgresql://<host>/<db>?sslmode=require
   spring.datasource.username=<username>
   spring.datasource.password=<password>
   jwt.secret=<your-secret-min-32-chars>
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

4. API is available at: `http://localhost:8080`

## Environment Variables / Properties

| Property | Description | Example |
|---|---|---|
| `spring.datasource.url` | Neon PostgreSQL JDBC URL | `jdbc:postgresql://...` |
| `spring.datasource.username` | DB username | `neondb_owner` |
| `spring.datasource.password` | DB password | `****` |
| `jwt.secret` | JWT signing secret (min 32 chars / 256 bits) | `observerr-secret...` |
| `jwt.expiration` | Access token expiry in milliseconds | `86400000` (24h) |
| `jwt.refresh-expiration` | Refresh token expiry in milliseconds | `604800000` (7 days) |
| `jwt.issuer` | JWT issuer claim | `observerr` |

## Authentication

All protected routes require an `Authorization` header:

```
Authorization: Bearer <access_token>
```

Tokens are obtained from the `/api/auth/login` or `/api/auth/register` endpoints.

## API Endpoints

### Auth

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Register a new user |
| `POST` | `/api/auth/login` | No | Login and receive tokens |
| `POST` | `/api/auth/refresh` | Refresh token | Get a new access token |
| `GET` | `/api/auth/me` | Access token | Get current user info |

### Student (requires `STUDENT` role)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/student/hello` | Health check for student role |

### Lecturer (requires `LECTURER` role)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/lecturer/hello` | Health check for lecturer role |

## Request / Response Examples

### Register

```json
POST /api/auth/register
{
  "fullName": "Dr. Kwame Mensah",
  "email": "kwame@university.edu",
  "password": "password123",
  "role": "LECTURER"
}
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

```json
POST /api/auth/login
{
  "email": "kwame@university.edu",
  "password": "password123"
}
```

Response `200 OK`: same shape as register.

### Error Responses

All errors follow this shape:
```json
{
  "error": "CONFLICT",
  "message": "Email already registered",
  "timestamp": "2026-07-02T10:00:00"
}
```

## HTTP Status Codes

| Code | Scenario |
|---|---|
| `201` | Successful registration |
| `200` | Successful login / refresh / info |
| `400` | Validation errors |
| `401` | Missing / invalid token, wrong credentials |
| `403` | Valid token but wrong role |
| `409` | Email already registered |
| `500` | Unexpected server error |

## Security Design

- Passwords are hashed with BCrypt (cost factor 12) — never stored plain
- JWT access tokens expire in 24h; refresh tokens expire in 7 days
- Same error message for wrong email and wrong password ("Invalid credentials") — prevents user enumeration
- Stateless sessions — no `HttpSession` anywhere
- CSRF disabled (stateless JWT APIs do not need it)
- CORS configured for `http://localhost:5173` (React dev server)

## Postman Collection

Import `postman/Observerr_Auth.postman_collection.json` into Postman to get a ready-to-run collection with all endpoints and test scripts.
