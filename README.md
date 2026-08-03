# Observerr Backend

Spring Boot backend for **Observerr** — an online exam integrity monitoring platform with JWT auth, role-based access (Student / Lecturer / Admin), proctoring session ingest, and lecturer analytics.

| Environment | URL |
|---|---|
| Production | `https://observerr-production.up.railway.app` |
| Frontend | `https://observerr-ui.pages.dev` |
| Local | `http://localhost:8080` |

---

## Frontend integration (start here)

### Auth

All protected routes require:

```
Authorization: Bearer <accessToken>
```

Obtain tokens via `POST /api/auth/login`. Refresh via `POST /api/auth/refresh` (send refresh token in `Authorization` header).

### Demo accounts (Neon)

| Role | Institutional ID | Use for |
|---|---|---|
| Student | `STU-12345` | Results, exams, integrity ingest |
| Lecturer | `STU-67890` | Exams, students, analytics |

Ask backend team for passwords.

### Postman

Import both files from `postman/`:

1. `Observerr_Auth.postman_collection.json` — all endpoints
2. `Observerr.postman_environment.json` — `baseUrl` + token variables

See `postman/README.md` for test flows.

### Standard error response

```json
{
  "error": "NOT_FOUND",
  "message": "Exam not found",
  "timestamp": "2026-07-28T12:00:00"
}
```

Validation errors (`400`):

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "errors": { "fieldName": "error message" },
  "timestamp": "2026-07-28T12:00:00"
}
```

| Status | Meaning |
|---|---|
| `401` | Missing/expired token |
| `403` | Wrong role (e.g. student hitting `/api/lecturer/**`) |
| `404` | Resource not found |
| `409` | Conflict (duplicate register, session already in progress) |
| `500` | Server error — check Railway logs; run **V13 / V14** manual seeds on Neon if analytics/dashboard tables are empty (see below) |

### Key response shapes

**Login** (`POST /api/auth/login` → `200`):

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "role": "LECTURER",
  "institutionalId": "STU-67890",
  "expiresIn": 86400000
}
```

**Student exams list** (`GET /api/student/exams` → `200`) — every **published** exam (not limited to prior enrollments; **V15** backfills enrollments on deploy):

```json
{
  "exams": [{
    "id": 1,
    "title": "Midterm Exam",
    "courseLabel": "CS201: Data Structures",
    "status": "LIVE",
    "canTake": true,
    "security": { "webcamMonitoring": true, "tabSwitchTracking": true, "blockCopyPaste": false }
  }],
  "totalElements": 3
}
```

`canTake` is `true` only while the exam window is **LIVE** (schedule + lecturer start). Unpublished exams are omitted from this list.

**Analytics overview** (`GET /api/lecturer/analytics/overview?period=7D` → `200`):

```json
{
  "period": "7D",
  "totalExamsMonitored": { "value": 312, "changePercent": 8.0, "changeDirection": "UP", "changeLabel": "from last week" },
  "totalFlaggedEvents": { "value": 89, "changePercent": 3.0, "changeDirection": "UP", "changeLabel": "from last week" },
  "avgIntegrityScore": { "value": 95.1, "changePercent": 0.4, "changeDirection": "UP", "changeLabel": "vs last week" },
  "mostCommonFlag": { "label": "Face Not Visible", "sharePercent": 45, "icon": "visibility_off" },
  "trends": {
    "title": "Integrity Event Trends",
    "subtitle": "Daily flagged events vs monitored sessions",
    "granularity": "DAY",
    "points": [{ "label": "Thu", "monitoredSessions": 60, "flaggedEvents": 28, "alert": true }]
  },
  "topBehaviors": [{ "behaviorCode": "FACE_NOT_VISIBLE", "label": "Face Not Visible", "eventCount": 43, "icon": "visibility_off", "tone": "error" }]
}
```

> **Analytics `period`:** only `7D`, `30D`, `3M` — `Custom` is not supported yet.

**Lecturer dashboard** (`GET /api/lecturer/dashboard` → `200`):

```json
{
  "liveExam": {
    "examId": 1,
    "title": "Advanced Calculus Final",
    "courseCode": "MATH401",
    "status": "LIVE",
    "remainingSeconds": 5400,
    "activeStudents": 4,
    "highRiskCount": 2,
    "avgIntegrityScore": 88.0,
    "liveMonitoringPath": "/lecturer/exams/1/live"
  },
  "needsReview": [],
  "examTabs": { "live": [], "upcoming": [], "completed": [] },
  "integrityTrend": { "changeLabel": "+0.4% vs last week", "changeDirection": "UP", "points": [12, 28, 15] },
  "topFlaggedBehaviors": []
}
```

**Integrity session start** (`POST /api/student/exams/{examId}/sessions` → `201`):

```json
{
  "sessionId": "uuid",
  "examId": 1,
  "status": "IN_PROGRESS",
  "startingScore": 100
}
```

Session IDs are **UUIDs** for live ingest. Legacy demo sessions use **numeric** IDs on the lecturer timeline endpoint.

**Start session rules:** the student must be enrolled and the exam must be published, LIVE, inside its configured time window, and have webcam monitoring enabled. Only one **IN_PROGRESS** session per student per exam is allowed.

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
- Maven (wrapper included: `./mvnw`)
- **Optional:** Node.js 18+ only if you run manual Neon seed scripts in `scripts/` (see `scripts/README.md`). The deployed app is **Java only**.

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
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | Web Push (optional; no-op client if unset) |

See gitignored `env.md` for a full Railway Raw Editor template.

### Deploy troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Healthcheck failed | App crash on startup — often Flyway not running (Boot 4 needs `spring-boot-starter-flyway`) or schema behind (missing `exam_answers`) | Check **Deploy Logs** for `Schema validation: missing table`; redeploy with Flyway starter; or run `node scripts/run-exam-core-migration.mjs` against Neon |
| `missing table [...]` | Migration not applied | Run manual seed scripts (below), then redeploy |
| Build OK, deploy “upstream issues” | Railway infra glitch | Redeploy; production may still run on previous replica |
| `401` on all routes | Missing / wrong `JWT_SECRET` | Set variable and redeploy |

---

## Database & migrations

Schema is managed by **Flyway** (`V1`–`V19`). Hibernate defaults to **`ddl-auto=validate`** — it does not create tables.

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
| V14 | Dashboard demo `exam_sessions` for lecturer home / live monitoring |
| V15 | Historical broad enrollment backfill (new exams use explicit enrollment) |
| V16 | Exam questions, options, answers, results |
| V17 | Notification inbox/preferences, exam student blocks, active-attempt index |
| V18 | Web Push subscriptions (`endpoint` / `p256dh` / `auth`) replace FCM tokens |
| V19 | Backfill missing `first_name` / `last_name` for legacy signups |

If Flyway did not run on Neon (legacy DB), apply manually (optional Node — see [`scripts/README.md`](scripts/README.md)):

```bash
# Set DATABASE_URL or SPRING_DATASOURCE_* (see scripts/db-connection.mjs)
# First time only: cd scripts && npm install && cd ..
node scripts/run-exam-sessions-migration.mjs   # V11
node scripts/run-student-exams-seed.mjs        # V12
node scripts/run-lecturer-analytics-seed.mjs   # V13
node scripts/run-lecturer-dashboard-seed.mjs   # V14
# Optional: node scripts/verify-analytics-coverage.mjs — sanity-check V13 analytics rows
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

`/api/admin/**` is reserved in security config; there are no admin REST controllers in this repo yet.

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
| `GET` | `/` | All **published** exams (any student; not limited to prior enrollments) |
| `GET` | `/{examId}` | Single published exam detail (`canTake`, `security`, schedule) |

New published exams auto-enroll all student accounts when created with `publish: true`.

### Integrity ingest — (STUDENT)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/student/exams/{examId}/sessions` | Start proctoring session → UUID `sessionId` |
| `POST` | `/api/student/exam-sessions/{sessionId}/integrity-events` | Batch append events (idempotent on `clientEventId`) |
| `POST` | `/api/student/exam-sessions/{sessionId}/media-token` | Short-lived LiveKit publish-only token |
| `POST` | `/api/student/exam-sessions/{sessionId}/complete` | Complete session + final summary |

Integrity event codes and deductions are canonicalized by the server (`IntegrityScoringPolicy`). Client-supplied score fields are ignored during ingest. Per-type deduction caps apply (e.g. **COPY = 7 pts, max 35** → five copies land at **65%** and set `requiresReview`). Risk tiers: **LOW ≥ 71**, **MEDIUM 31–70**, **HIGH ≤ 30**; sessions with `requiresReview` surface as HIGH on live monitoring. If proctoring is unavailable, the final score is capped at **60** and the session requires lecturer review. Start session **resumes** an existing `IN_PROGRESS` attempt (refresh-safe).

Live monitoring `stats.active` counts only `IN_PROGRESS` sessions — enrolled but not-started students appear in `total`, not `active`.

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
| `POST` | `/` | Create exam (`publish: true` enrolls all student users) |
| `POST` | `/{examId}/start` | Transition exam to LIVE + Web Push notifications |

### Lecturer analytics — `/api/lecturer/analytics` (LECTURER)

| Method | Path | Description |
|---|---|---|
| `GET` | `/overview?period=7D\|30D\|3M` | Analytics dashboard (KPIs, trends, top behaviors) |

### Lecturer dashboard — `/api/lecturer/dashboard` (LECTURER)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Aggregated home page: live exam banner, needs review, exam tabs, 7D trend slice, top behaviors |

Always returns **200** with empty arrays / `liveExam: null` when no data (not 404).

### Lecturer live monitoring — `/api/lecturer/exams` (LECTURER)

| Method | Path | Description |
|---|---|---|
| `GET` | `/{examId}/live-sessions` | Enrolled students + in-progress `exam_sessions`, stats for live monitoring UI |

### Lecturer proctoring (metadata v1) — `/api/lecturer/proctoring` (LECTURER)

| Method | Path | Description |
|---|---|---|
| `GET` | `/exams` | Live exams with active session feed counts |
| `GET` | `/exams/{examId}/feeds` | Active session metadata (`snapshotUrl` null until thumbnails exist) |
| `POST` | `/exams/{examId}/media-token` | Short-lived LiveKit subscribe-only token for the exam owner |

### Lecturer students — extra

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/lecturer/students/needs-review?limit=10&examId=` | Optional; same rows as dashboard `needsReview` |

### Devices — `/api/devices` (authenticated)

| Method | Path | Description |
|---|---|---|
| `POST` | `/token` | Register Web Push subscription `{ endpoint, keys: { p256dh, auth } }` |
| `DELETE` | `/token` | Unregister the same subscription body |

### Notifications — `/api/notifications` (authenticated)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Inbox page (`page`, `size`, `category`, `unreadOnly`) |
| `PATCH` | `/{id}/read` | Mark one notification read |
| `POST` | `/read-all` | Mark all read |
| `GET` / `PUT` | `/preferences` | Notification preference toggles |

### Integrity deduction reference (server)

| Cap key | Per event | Max | Notes |
|---|---:|---:|---|
| COPY | 7 | 35 | 5× → 65%; hitting cap flags review |
| PASTE | 8 | 32 | Stronger than copy |
| MULTI_FACE | 15 | 45 | Always review |
| FACE_ABSENT | 8 | 32 | Short/medium/long share this cap |
| DEVTOOLS | 10 | 30 | Incl. Alt/Cmd+Tab shortcut attempts |
| TAB_SWITCH | 5 | 25 | `document.hidden` |
| FOCUS_LOSS | 4 | 20 | Window blur while tab visible |
| FULLSCREEN_EXIT | 5 | 15 | |
| PAGE_REFRESH | 6 | 18 | |
| IDLE | 3 | 15 | Idle ≥ 60s |
| GAZE | 3 | 15 | All gaze durations share this cap |

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
│   ├── students/      Roster + session timeline + needs-review
│   ├── analytics/     Analytics overview API
│   └── dashboard/     Home dashboard, live sessions, proctoring metadata
├── notification/      Web Push + device subscriptions
├── config/            Security, CORS, cache, Firebase, Cloudinary
└── exception/         Global error handling

src/main/resources/db/migration/   Flyway SQL (V1–V17)
scripts/                           Optional Neon seed runners (.mjs); Node not used at runtime
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
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | No | — | Web Push; disabled if empty |
| `CLOUDINARY_CLOUD_NAME` | No | — | Profile pictures |
| `CLOUDINARY_API_KEY` | No | — | Profile pictures |
| `CLOUDINARY_API_SECRET` | No | — | Profile pictures |
| `APP_FRONTEND_BASE_URL` | No | Pages dev URL | Web Push deep links |
| `AUTH_COOKIE_SECURE` | No | `false` | Cookie `Secure` flag |
| `AUTH_COOKIE_SAME_SITE` | No | `Lax` | Cookie `SameSite` |
| `JWT_EXPIRATION` | No | 86400000 | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRATION` | No | 604800000 | Refresh token TTL (ms) |
| `LIVEKIT_URL` | For live media | — | LiveKit Cloud WebSocket URL (`wss://...`) |
| `LIVEKIT_API_KEY` | For live media | — | LiveKit project API key |
| `LIVEKIT_API_SECRET` | For live media | — | LiveKit project secret (at least 32 bytes) |
| `LIVEKIT_TOKEN_TTL_SECONDS` | No | 300 | Media token TTL, clamped to 30–900 seconds |

\*Required in production.

---

## Security notes

- Passwords: BCrypt (cost 12)
- Stateless JWT sessions; optional Redis refresh-token blocklist
- CSRF disabled (token API)
- `/health` and auth routes are public; role enforced on `/api/student/**`, `/api/lecturer/**`
- LiveKit secrets remain server-side; students receive room-scoped publish-only grants and exam owners receive subscribe-only grants.
- LiveKit token issuance fails closed when credentials are absent or invalid.

---

## Postman

| File | Purpose |
|---|---|
| `postman/Observerr_Auth.postman_collection.json` | Full API collection (~40 requests) |
| `postman/Observerr.postman_environment.json` | `baseUrl`, token, `examId`, `sessionId` vars |
| `postman/README.md` | Import steps and test flows |

Set `baseUrl` to production or `http://localhost:8080`. Run **Login (Demo Lecturer)** or **Login (Demo Student)** first — tokens are saved automatically.
