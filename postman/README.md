# Postman — Observerr API

## Import

1. **Collection:** `Observerr_Auth.postman_collection.json`
2. **Environment:** `Observerr.postman_environment.json`
3. Select the **Observerr** environment (top-right dropdown)

## Quick test flow

### Student flow
1. **Auth → Login (Demo Student)** — saves `accessToken`
2. **Student → List Exams**
3. **Student → Get Exam Detail** — uses `{{examId}}`
4. **Integrity → Start Session** — saves `{{sessionId}}`
5. **Integrity → Batch Events**
6. **Integrity → Complete Session**

### Lecturer flow
1. **Auth → Login (Demo Lecturer)** — saves `accessToken`
2. **Lecturer → Dashboard Home**
3. **Lecturer → Live Sessions** — set `examId` to live exam
4. **Lecturer → List Exams**
3. **Lecturer → List Students**
4. **Lecturer → Analytics Overview (7D)**
5. **Lecturer → Session Detail** — UUID or numeric legacy ID

## Variables

| Variable | Set by | Used for |
|---|---|---|
| `baseUrl` | Environment | All requests |
| `accessToken` | Login / Register | `Authorization: Bearer` |
| `refreshToken` | Login / Register | Refresh endpoint |
| `examId` | Manual or Create Exam | Path params |
| `sessionId` | Start Session | Integrity endpoints |

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

Validation errors include an `errors` object instead of/in addition to `message`.
