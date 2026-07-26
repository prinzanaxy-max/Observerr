# Observerr Backend

Spring Boot backend for **Observerr** — an online exam integrity monitoring platform with JWT auth, role-based access (Student / Lecturer / Admin), proctoring session ingest, and lecturer analytics.

| Environment | URL |
|---|---|
| Production | `https://observerr-production.up.railway.app` |
| Frontend | `https://observerr-ui.pages.dev` |
| Local | `http://localhost:8080` |

---

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Database | PostgreSQL (Neon) |
| Migrations | Flyway (`src/main/resources/db/migration`) |
| ORM | Spring Data JPA / Hibernate 7 |
| Cache | Caffeine (student results) |
| Push | Firebase Cloud Messaging (optional) |
| Media | Cloudinary (profile pictures, optional) |
| Build / deploy | Maven, Docker, Railway |

---

## Quick start (local)

### Prerequisites

- Java 21+
- Node.js 18+ (for manual Neon seed scripts only)
- A Neon PostgreSQL database (or PostgreSQL 14+)

### 1. Environment variables

Set at minimum:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://<host>/<db>?sslmode=require"
export SPRING_DATASOURCE_USERNAME="<username>"
export SPRING_DATASOURCE_PASSWORD="<password>"
export JWT_SECRET="<min-32-chars>"
```

On Windows, use `run-local.ps1` (gitignored) or copy values from your local `env.md`.

### 2. Run

```bash
./mvnw spring-boot:run
```

Health check: `GET http://localhost:8080/health`

### 3. Tests

```bash
./mvnw test
```

---

## Deployment (Railway)

- **Builder:** Dockerfile (`railway.toml`)
- **Health check:** `GET /health` (120s timeout)
- **Port:** Railway sets `PORT`; app binds via `server.port=${PORT:8080}`

### Required Railway variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | Neon JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `JWT_SECRET` | Min 32 characters |

### Recommended production variables

| Variable | Description |
|---|---|
| `SPRING_WEB_CORS_ALLOWED_ORIGINS` | e.g. `https://observerr-ui.pages.dev` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `validate` (default — Flyway owns schema) |
| `REDIS_URL` | Upstash Redis TCP URL (refresh-token blocklist) |
| `AUTH_COOKIE_SECURE` | `true` |
| `AUTH_COOKIE_SAME_SITE` | `None` (cross-site cookies with HTTPS frontend) |
| `CLOUDINARY_*` | Profile picture uploads |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | FCM push (optional; no-op client if unset) |

See gitignored `env.md` for a full Railway Raw Editor template.

### Deploy troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Healthcheck failed | App crash on startup | Check **Deploy Logs** for Flyway / schema validation errors |
| `missing table [...]` | Migration not applied | Run manual seed scripts (below), then redeploy |
| Build OK, deploy “upstream issues” | Railway infra glitch | Redeploy; production may still run on previous replica |
| `401` on all routes | Missing / wrong `JWT_SECRET` | Set variable and redeploy |

---

## Database & migrations

Schema is managed by **Flyway** (`V1`–`V13`). Hibernate defaults to **`ddl-auto=validate`** — it does not create tables.

| Migration | Purpose |
|---|---|
| V1–V5 | Users, profile columns |
| V4 | Exams, enrollments, device tokens |
| V6–V8 | Student completed assessments + demo seed |
| V9 | Lecturer courses, proctoring sessions, demo students |
| V10 | Lecturer exams extension + demo exams |
| V11 | `exam_sessions`, `integrity_events` (live proctoring ingest) |
| V12 | Enroll demo student `STU-12345` in published exams |
| V13 | Lecturer analytics overview seed data |

If Flyway did not run on Neon (legacy DB), apply manually:

```bash
# Set DATABASE_URL or SPRING_DATASOURCE_* (see scripts/db-connection.mjs)
node scripts/run-exam-sessions-migration.mjs   # V11
node scripts/run-student-exams-seed.mjs        # V12
node scripts/run-lecturer-analytics-seed.mjs   # V13
node scripts/run-neon-seed.mjs                 # student results demo
node scripts/run-lecturer-seed.mjs             # lecturer students demo
node scripts/run-lecturer-exams-seed.mjs       # lecturer exams demo
```

Scripts accept either `DATABASE_URL` or `SPRING_DATASOURCE_URL` + `SPRING_DATASOURCE_USERNAME` + `SPRING_DATASOURCE_PASSWORD`.

---

## Authentication

Protected routes:

```
Authorization: Bearer <accessToken>
```

Obtain tokens from `POST /api/auth/login` or `POST /api/auth/register`.

| Role | Prefix |
|---|---|
| Student | `/api/student/**` |
| Lecturer | `/api/lecturer/**` |
| Admin | `/api/admin/**` |

---

## API reference

### Health

| Method | Path | Auth |
|---|---|---|
| `GET` | `/health` | No |

### Auth — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/register` | No | Register |
| `POST` | `/login` | No | Login |
| `POST` | `/refresh` | Refresh token | New access token |
| `POST` | `/logout` | Access token | Logout |
| `GET` | `/me` | Access token | Current user |

### Account — `/api/account`

| Method | Path | Description |
|---|---|---|
| `GET` | `/me` | Profile |
| `PATCH` | `/me` | Update profile |
| `PATCH` | `/me/password` | Change password |
| `POST` | `/me/profile-picture` | Upload avatar (multipart) |
| `DELETE` | `/me/profile-picture` | Remove avatar |

### Student results — `/api/student/results` (STUDENT)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Paginated completed assessments (`page`, `size`, `sort`) |
| `GET` | `/summary` | Dashboard summary counts |

### Student stats — `/api/student/stats` (STUDENT)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Dashboard cards (exams completed, avg integrity, etc.) |

### Student exams — `/api/student/exams` (STUDENT)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Enrolled published exams |
| `GET` | `/{examId}` | Single exam detail (`canTake`, `security`, schedule) |

### Integrity ingest — (STUDENT)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/student/exams/{examId}/sessions` | Start proctoring session → UUID `sessionId` |
| `POST` | `/api/student/exam-sessions/{sessionId}/integrity-events` | Batch append events (idempotent on `clientEventId`) |
| `POST` | `/api/student/exam-sessions/{sessionId}/complete` | Complete session + final summary |

### Lecturer students — `/api/lecturer/students` (LECTURER)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Roster (`page`, `size`, `search`, `course`) |
| `GET` | `/sessions/{sessionId}` | Session timeline (UUID = live sessions, numeric = legacy demo) |

### Lecturer exams — `/api/lecturer/exams` (LECTURER)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | List exams (`status`, `search`) |
| `GET` | `/{examId}` | Exam detail |
| `POST` | `/` | Create exam |
| `POST` | `/{examId}/start` | Transition exam to LIVE + notifications |

### Lecturer analytics — `/api/lecturer/analytics` (LECTURER)

| Method | Path | Description |
|---|---|---|
| `GET` | `/overview?period=7D\|30D\|3M` | Analytics dashboard (KPIs, trends, top behaviors) |

### Devices — `/api/devices` (authenticated)

| Method | Path | Description |
|---|---|---|
| `POST` | `/token` | Register FCM device token |

---

## Demo data (Neon)

After running seed scripts:

| Account | Institutional ID | Role |
|---|---|---|
| Demo student | `STU-12345` | STUDENT |
| Demo lecturer | `STU-67890` | LECTURER |

Legacy proctoring demo includes **Alex Mercer** (numeric session IDs). Live integrity sessions use **UUID** session IDs from `POST .../sessions`.

---

## Project structure

```
src/main/java/com/backend/observerr/
├── auth/              Login, JWT, users
├── account/           Profile, password, avatar
├── student/
│   ├── results/       Completed assessments API
│   ├── stats/         Dashboard stats
│   └── exams/         Student exam list/detail
├── integrity/         Exam sessions + integrity event ingest
├── exam/              Lecturer exam CRUD + lifecycle
├── lecturer/
│   ├── students/      Roster + session timeline
│   └── analytics/     Analytics overview API
├── notification/      FCM + device tokens
├── config/            Security, CORS, cache, Firebase, Cloudinary
└── exception/         Global error handling

src/main/resources/db/migration/   Flyway SQL (V1–V13)
scripts/                           Manual Neon seed runners (.mjs)
```

---

## Environment variables (full)

| Variable | Required | Default | Description |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Yes | — | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Yes | — | DB user |
| `SPRING_DATASOURCE_PASSWORD` | Yes | — | DB password |
| `JWT_SECRET` | Yes* | local dev fallback | Signing secret (min 32 chars) |
| `PORT` / `SERVER_PORT` | No | `8080` | HTTP port (Railway sets `PORT`) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | No | `validate` | Use `validate` with Flyway |
| `SPRING_WEB_CORS_ALLOWED_ORIGINS` | No | localhost + Pages dev | CORS origins |
| `REDIS_URL` | No | — | Refresh-token blocklist (in-memory if empty) |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | No | — | FCM; disabled if empty |
| `CLOUDINARY_CLOUD_NAME` | No | — | Profile pictures |
| `CLOUDINARY_API_KEY` | No | — | Profile pictures |
| `CLOUDINARY_API_SECRET` | No | — | Profile pictures |
| `APP_FRONTEND_BASE_URL` | No | Pages dev URL | FCM deep links |
| `AUTH_COOKIE_SECURE` | No | `false` | Cookie `Secure` flag |
| `AUTH_COOKIE_SAME_SITE` | No | `Lax` | Cookie `SameSite` |
| `JWT_EXPIRATION` | No | 86400000 | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRATION` | No | 604800000 | Refresh token TTL (ms) |

\*Required in production.

---

## Security notes

- Passwords: BCrypt (cost 12)
- Stateless JWT sessions; optional Redis refresh-token blocklist
- CSRF disabled (token API)
- `/health` and auth routes are public; role enforced on `/api/student/**`, `/api/lecturer/**`
- Integrity ingest trusts client `pointsDeducted` / `scoreAfter` for v1

---

## Postman

Import `postman/Observerr_Auth.postman_collection.json`. Set `baseUrl` to production or `http://localhost:8080`.
