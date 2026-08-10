# Observerr API — saved response examples

Canonical success/error bodies for Postman and clients. Timestamps abbreviated.

## Auth

### `POST /api/auth/register` — 201
```json
{
  "accessToken": "eyJ…",
  "refreshToken": "eyJ…",
  "tokenType": "Bearer",
  "role": "STUDENT",
  "institutionalId": "STU-001",
  "expiresIn": 86400
}
```

### `POST /api/auth/register` — 400 (lecturer / validation)
```json
{
  "error": "BAD_REQUEST",
  "message": "Public registration is available for students only",
  "timestamp": "2026-08-03T02:00:00"
}
```
```json
{
  "error": "VALIDATION_FAILED",
  "message": "Validation failed",
  "timestamp": "2026-08-03T02:00:00",
  "errors": {
    "firstName": "First name is required",
    "lastName": "Last name is required"
  }
}
```

### `POST /api/auth/register` — 409
```json
{
  "error": "CONFLICT",
  "message": "Institutional ID already registered",
  "timestamp": "2026-08-03T02:00:00"
}
```

### `POST /api/auth/login` — 200
Same shape as register 201.

### `POST /api/auth/login` — 401
```json
{
  "error": "UNAUTHORIZED",
  "message": "Invalid credentials",
  "timestamp": "2026-08-03T02:00:00"
}
```

### `GET /api/auth/me` — 401 (no token)
```json
{
  "error": "UNAUTHORIZED",
  "message": "Full authentication is required to access this resource",
  "timestamp": "2026-08-03T02:00:00"
}
```

---

## Integrity

### `POST /api/student/exams/{examId}/sessions` — 201 (new or resumed)
```json
{
  "sessionId": "6b86d8e5-1ed1-4583-9cc6-ca42dffdd650",
  "examId": 42,
  "studentId": 101,
  "startedAt": "2026-08-03T02:00:00Z",
  "startingScore": 100,
  "status": "IN_PROGRESS"
}
```

### Start session — 403 / 404 / 409
```json
{ "error": "FORBIDDEN", "message": "Student is not enrolled in this exam", "timestamp": "…" }
```
```json
{ "error": "NOT_FOUND", "message": "Exam not found", "timestamp": "…" }
```
```json
{ "error": "CONFLICT", "message": "Exam is not live", "timestamp": "…" }
```

### `POST …/integrity-events` — 200
```json
{
  "accepted": 2,
  "skipped": 1,
  "currentScore": 88,
  "requiresReview": false
}
```

### Five `COPY_EVENT`s (server caps at 35 → score 65, review flagged)
```json
{
  "accepted": 5,
  "skipped": 0,
  "currentScore": 65,
  "requiresReview": true
}
```

### Integrity events — 400 unknown code / 409 completed
```json
{ "error": "BAD_REQUEST", "message": "Unsupported integrity eventCode", "timestamp": "…" }
```
```json
{ "error": "CONFLICT", "message": "Session is already completed", "timestamp": "…" }
```

### `POST …/complete` — 200
```json
{
  "sessionId": "6b86d8e5-1ed1-4583-9cc6-ca42dffdd650",
  "finalScore": 65,
  "requiresReview": true,
  "status": "COMPLETED"
}
```

### Complete — 400 startingScore mismatch
```json
{
  "error": "BAD_REQUEST",
  "message": "Completion summary starting score does not match session",
  "timestamp": "…"
}
```

---

## Student results

### `GET /api/student/results` — 200
```json
{
  "content": [
    {
      "id": 7,
      "examId": 42,
      "examTitle": "Midterm",
      "academicScore": 80,
      "maxScore": 100,
      "integrityScore": 88,
      "status": "RELEASED",
      "submittedAt": "2026-08-03T02:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

### `GET /api/student/stats` — 200
```json
{
  "examsCompleted": 3,
  "avgIntegrity": 92.5,
  "verifiedSessions": 2,
  "underReview": 0
}
```

---

## Exam attempt

### `GET /api/student/exam-sessions/{sessionId}/questions` — 200
```json
[
  {
    "id": 1,
    "text": "What is 2+2?",
    "order": 1,
    "points": 10,
    "options": [
      { "choice": "A", "text": "3" },
      { "choice": "B", "text": "4" }
    ]
  }
]
```

### `PUT …/answers/{questionId}` — 200
```json
{ "questionId": 1, "selectedOption": "B", "savedAt": "2026-08-03T02:05:00Z" }
```

### `POST …/submit` — 200
Returns `ExamResultDto` (same fields as lecturer results list item).

---

## LiveKit media tokens

### `POST /api/student/exam-sessions/{sessionId}/media-token` — 200
```json
{
  "url": "wss://observerr-….livekit.cloud",
  "token": "eyJ…",
  "roomName": "exam-42",
  "participantIdentity": "student-101-session-…",
  "expiresAt": "2026-08-03T02:05:00Z"
}
```

### `POST /api/lecturer/proctoring/exams/{examId}/media-token` — 200
Same shape; subscribe-only grant for lecturer.

---

## Live monitoring

### `GET /api/lecturer/exams/{examId}/live-sessions` — enrolled 1, none started
```json
{
  "examId": 42,
  "stats": {
    "active": 0,
    "total": 1,
    "highRisk": 0,
    "warnings": 0,
    "networkStability": 94
  },
  "students": [
    {
      "studentId": 101,
      "studentNumber": "03099",
      "name": "Prinzanaxy 03099",
      "initials": "P0",
      "liveStatus": "NOT_STARTED",
      "liveStatusLabel": "Not joined",
      "riskLevel": "LOW",
      "lastEvent": "No events yet",
      "latestSessionId": null,
      "integrityScore": 100
    }
  ]
}
```

### Live sessions — one IN_PROGRESS
```json
{
  "examId": 42,
  "stats": { "active": 1, "total": 1, "highRisk": 0, "warnings": 0, "networkStability": 88 },
  "students": [
    {
      "liveStatus": "ACTIVE",
      "liveStatusLabel": "Active - Monitoring",
      "integrityScore": 88,
      "riskLevel": "LOW"
    }
  ]
}
```

---

## Devices (Web Push)

### `POST /api/devices/token` — 200
```json
{ "success": true, "message": "Push subscription registered" }
```

### `DELETE /api/devices/token` — 204
Empty body.

### Devices — 400 validation
```json
{
  "error": "VALIDATION_FAILED",
  "message": "Validation failed",
  "errors": { "endpoint": "Push endpoint is required" },
  "timestamp": "…"
}
```

---

## Notifications

### `GET /api/notifications` — 200
```json
{
  "content": [
    {
      "id": 1,
      "category": "RESULT",
      "title": "Result Released",
      "message": "Your exam result is now available.",
      "read": false,
      "createdAt": "2026-08-03T02:00:00Z",
      "deepLink": "/student/results/7"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "unreadCount": 1
}
```

### `GET /api/notifications/preferences` — 200
```json
{
  "examEvents": true,
  "integrityAlerts": true,
  "resultUpdates": true,
  "systemUpdates": true
}
```

### `PATCH /api/notifications/{id}/read` — 204
Empty body.

### `PATCH /api/notifications/read-all` — 204
Empty body.

### `DELETE /api/notifications/{id}` — 204
Empty body.

---

### `GET /health` — 200
```json
{
  "status": "UP",
  "app": "Observerr Backend",
  "timestamp": "2026-08-03T02:00:00",
  "version": "1.0.0",
  "uptimeSeconds": 120
}
```

### `GET /ready` — 200 (webpush optional)
```json
{
  "status": "UP",
  "checks": {
    "database": "UP",
    "redis": "OPTIONAL_DISABLED",
    "webpush": "CONFIGURED"
  },
  "timestamp": "…"
}
```
