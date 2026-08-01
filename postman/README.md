# Postman — Observerr API

> Backend runtime is **Java / Maven / Docker** only. Node is not required for Postman or the API.

## Import

1. **Collection:** `Observerr_Auth.postman_collection.json`
2. **Environment:** `Observerr.postman_environment.json`
3. Select the **Observerr** environment (top-right dropdown)

The collection includes **~40 requests** covering health, auth, account, student, integrity ingest, lecturer exams/dashboard/students/analytics/proctoring, devices, and sample error cases.

## Quick test flow

### Student flow
1. **Auth → Login (Demo Student)** — saves `accessToken` / `refreshToken`
2. **Student → List Exams** — all **published** exams; test script sets `examId` (prefers a **LIVE** exam)
3. **Student → Get Exam Detail** — uses `{{examId}}`
4. **Integrity (Student) → Start Session** — requires a **published** exam; saves UUID `{{sessionId}}`
5. **Integrity → Batch Integrity Events**
6. **Integrity → Complete Session**

### Lecturer flow
1. **Auth → Login (Demo Lecturer)** — saves tokens
2. **Lecturer — Dashboard → Dashboard Home**
3. **Lecturer — Dashboard → Live Sessions** — set `examId` to a live exam (or use value from **List Exams**)
4. **Lecturer — Exams → List Exams**
5. **Lecturer — Students → List Students**
6. **Lecturer — Analytics → Overview (7D)**
7. **Lecturer — Students → Session Detail** — UUID (live ingest) or numeric legacy demo ID

**Create & go live:** **Lecturer — Exams → Create Exam** with `"publish": true` enrolls all student accounts and saves `examId`. Use **Start Exam (Go Live)** before students get `canTake: true`.

## Variables

| Variable | Set by | Used for |
|---|---|---|
| `baseUrl` | Environment | All requests |
| `accessToken` | Login / Register | Collection bearer auth |
| `refreshToken` | Login / Register | `POST /api/auth/refresh` |
| `examId` | List Exams, Create Exam, manual | Student/lecturer exam paths |
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

Validation errors (`400`) include an `errors` object. Common codes: `401` (auth), `403` (wrong role), `404` (missing resource / unpublished exam on session start), `409` (session already in progress).

## API map (matches codebase)

| Folder | Base path |
|---|---|
| Health | `GET /health` |
| Auth | `/api/auth/*` |
| Account | `/api/account/*` |
| Student | `/api/student/results`, `/stats`, `/exams` |
| Integrity | `/api/student/exams/{examId}/sessions`, `/api/student/exam-sessions/{sessionId}/*` |
| Lecturer — Exams | `/api/lecturer/exams` (+ `/{examId}/start`, `/{examId}/live-sessions`) |
| Lecturer — Dashboard | `GET /api/lecturer/dashboard`, needs-review, live-sessions |
| Lecturer — Proctoring | `/api/lecturer/proctoring/exams` |
| Lecturer — Analytics | `GET /api/lecturer/analytics/overview?period=7D\|30D\|3M` |
| Devices | `POST /api/devices/token` |

Full tables: root [README.md](../README.md#api-reference).
