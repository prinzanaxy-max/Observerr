# Postman — Observerr API

> Backend runtime is **Java / Maven / Docker** only. Node is not required for Postman or the API.

## Import

1. **Collection:** `Observerr_Auth.postman_collection.json`
2. **Environment:** `Observerr.postman_environment.json`
3. Select the **Observerr** environment (top-right dropdown)
4. **Response catalog:** [`RESPONSE_EXAMPLES.md`](./RESPONSE_EXAMPLES.md) — success/error bodies for every major case

The collection covers health, auth (incl. named signup), account, student, integrity ingest (copy/tab caps), lecturer exams/dashboard/students/analytics/proctoring, devices (Web Push), notifications, and sample error cases.

## Quick test flow

### Student flow
1. **Auth → Login (Demo Student)** — saves `accessToken` / `refreshToken`
2. **Student → List Exams** — sets `examId` (prefers **LIVE**)
3. **Integrity → Start Session** — creates or **resumes** `IN_PROGRESS` session → `sessionId`
4. **Integrity → Batch Integrity Events** — try `COPY_EVENT` (7 pts, cap 35)
5. **Integrity → Complete Session**

### Lecturer flow
1. **Auth → Login (Demo Lecturer)**
2. **Dashboard → Live Sessions** — `stats.active` is **0** until a student has an `IN_PROGRESS` session
3. **Exams → Create Exam** (`publish: true`) → **Start Exam (Go Live)**
4. **Devices / Notifications** — Web Push subscription + inbox preferences

## Variables

| Variable | Set by | Used for |
|---|---|---|
| `baseUrl` | Environment | All requests |
| `accessToken` | Login / Register | Collection bearer auth |
| `refreshToken` | Login / Register | `POST /api/auth/refresh` |
| `examId` | List Exams, Create Exam | Student/lecturer exam paths |
| `sessionId` | Start Session | Integrity batch + complete |

## Switch environments

| Environment | `baseUrl` |
|---|---|
| Production | `https://observerr-production.up.railway.app` |
| Local | `http://localhost:8080` |

## Demo accounts (Neon)

| Role | Institutional ID |
|---|---|
| Student | `STU-12345` |
| Lecturer | `STU-67890` |

Passwords: ask the backend team.

## Error format

```json
{
  "error": "NOT_FOUND",
  "message": "Exam not found",
  "timestamp": "2026-07-28T12:00:00"
}
```

Validation errors (`400`) include an `errors` object. Common codes: `401` (auth), `403` (wrong role), `404` (missing resource), `409` (exam not live / session completed).

## API map

| Folder | Base path |
|---|---|
| Health | `GET /health`, `GET /ready` |
| Auth | `/api/auth/*` (register requires `firstName` / `lastName`) |
| Account | `/api/account/*` |
| Student | `/api/student/results`, `/stats`, `/exams` |
| Integrity | `/api/student/exams/{examId}/sessions`, `/api/student/exam-sessions/{sessionId}/*` |
| Lecturer — Exams | `/api/lecturer/exams` (+ start, live-sessions) |
| Lecturer — Dashboard | `GET /api/lecturer/dashboard` |
| Lecturer — Proctoring | `/api/lecturer/proctoring/exams` |
| Lecturer — Analytics | `GET /api/lecturer/analytics/overview` |
| Devices | `POST` / `DELETE` `/api/devices/token` (Web Push subscription) |
| Notifications | `/api/notifications`, `/preferences` |

Full tables + integrity deduction caps: root [README.md](../README.md#api-reference).
