# Postman — Observerr API

> Backend runtime is **Java / Maven / Docker** only. Node is not required for Postman or the API.

## Import

1. **Collection:** `Observerr_Auth.postman_collection.json`
2. **Environment:** `Observerr.postman_environment.json`
3. Select the **Observerr** environment (top-right dropdown)
4. **Response catalog:** [`RESPONSE_EXAMPLES.md`](./RESPONSE_EXAMPLES.md)

The collection mirrors implemented REST routes: health/ready, auth, account, student results & exams, integrity ingest + LiveKit student token, exam attempt (autosave/submit), lecturer exam lifecycle (questions, results, blocks), dashboard, proctoring + lecturer media token, analytics (overview + integrity-events), Web Push devices, notifications inbox, and sample error cases.

## Quick test flow

### Student flow
1. **Auth → Login (Demo Student)**
2. **Student → List Exams** → sets `examId`
3. **Lecturer — Exams → Replace Questions** (as lecturer) if the exam has no items yet
4. **Integrity → Start Session** → `sessionId` (LIVE exam, enrolled, webcam on)
5. **Exam Attempt → Session Questions** → sets `questionId`
6. **Exam Attempt → Autosave Answer** / **Submit Exam** (or **Integrity → Complete Session** for integrity-only path)
7. **Integrity → Student Media Token** (needs `LIVEKIT_*` on server)

### Lecturer flow
1. **Auth → Login (Demo Lecturer)**
2. **Exams → Create Exam** (`publish: true`) → **Replace Questions** → **Start Exam (Go Live)**
3. **Dashboard → Live Sessions** — `stats.active` counts **IN_PROGRESS** sessions only
4. **Exams → List Exam Results** → **Release Results**
5. **Proctoring → Lecturer Media Token**
6. **Analytics → Overview (7D)** / **Integrity Events Report**

## Variables

| Variable | Set by | Used for |
|---|---|---|
| `baseUrl` | Environment | All requests |
| `accessToken` | Login / Register | Collection bearer auth |
| `refreshToken` | Login / Register | `POST /api/auth/refresh` |
| `examId` | List Exams, Create Exam | Exam paths |
| `sessionId` | Start Session | Integrity + attempt |
| `resultId` | List Results | Result detail / analysis |
| `questionId` | Session Questions | Autosave / submit |
| `studentIdentifier` | Environment default | Block / unblock (`STU-12345`) |
| `notificationId` | List Notifications | Mark read / delete |

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

Validation errors (`400`) include an `errors` object. Common codes: `401` (auth), `403` (wrong role / not enrolled / blocked), `404` (missing resource), `409` (exam not live / session completed).

## API map

| Folder | Base path |
|---|---|
| Health | `GET /health`, `GET /ready` |
| Auth | `/api/auth/*` (register requires `firstName` / `lastName`; STUDENT only) |
| Account | `/api/account/*` |
| Student | `/api/student/results`, `/stats`, `/exams` (+ `/{examId}/questions`) |
| Integrity | `/api/student/exams/{examId}/sessions`, `/api/student/exam-sessions/{sessionId}/*` (+ `media-token`) |
| Exam Attempt | `/api/student/exam-sessions/{sessionId}/questions`, `/answers`, `/submit` |
| Lecturer — Exams | `/api/lecturer/exams` (+ publish, enrollments, questions, results, start/end, blocks, live-sessions) |
| Lecturer — Dashboard | `GET /api/lecturer/dashboard`, needs-review |
| Lecturer — Proctoring | `/api/lecturer/proctoring/exams` (+ `media-token`) |
| Lecturer — Analytics | `overview`, `integrity-events` |
| Devices | `POST` / `DELETE` `/api/devices/token` (Web Push subscription JSON) |
| Notifications | `/api/notifications`, `/preferences`, mark read, read-all, delete |

Full tables + integrity deduction caps: root [README.md](../README.md#api-reference).
