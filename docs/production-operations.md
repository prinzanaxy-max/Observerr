# Production operations

## Deployment and migrations

1. Back up PostgreSQL and record the currently deployed application image and Flyway version.
2. Run `./mvnw test` and `./mvnw package` in CI.
3. Apply Flyway migrations once from a release job or a single canary instance. Do not let many
   replicas race to perform a large migration.
4. Verify `/ready` reports PostgreSQL and Redis `UP`, then deploy the remaining replicas.
5. Smoke-test auth, enrollment-scoped exam visibility, session start, autosave, integrity ingest,
   submission, result release, notification inbox, and block enforcement.

Flyway migrations are forward-only. Never edit an applied migration or run `flyway repair` merely
to bypass a checksum mismatch. For a schema defect, stop rollout and restore the pre-deploy backup
if no new production writes must be retained. Otherwise deploy a new compensating migration and
an application version compatible with both states. Destructive changes require an
expand/backfill/contract sequence across separate releases.

V15 was a historical broad-enrollment backfill. New exam enrollment is explicit: pass
`studentInstitutionalIds` when creating an exam or use
`PUT /api/lecturer/exams/{examId}/enrollments`. Do not add another global student cross-join.

## Required production configuration

- Activate the `prod` or `production` Spring profile.
- Set a unique `JWT_SECRET` of at least 32 characters and `AUTH_COOKIE_SECURE=true`.
- Set `REDIS_URL`. Production startup rejects an absent URL, and `/ready` verifies connectivity.
  Local development and tests safely use the process-local limiter when Redis is absent.
- Size `DB_POOL_MAX_SIZE` so `(replicas × max pool) + migration/admin headroom` stays below the
  database connection limit. Configure the remaining `DB_POOL_*` values for the provider.
- Tune the `RATE_LIMIT_*` variables from observed traffic. Defaults allow one integrity batch
  every 12 seconds with retry headroom and high-frequency answer autosaves.

## PostgreSQL integration tests

`PostgresMigrationIntegrationTest` applies every migration to PostgreSQL 16 through Testcontainers
and validates PostgreSQL-specific tables and the partial active-attempt index. It is skipped only
when Testcontainers cannot find a usable Docker environment. CI intended as a release gate must
provide Docker and should fail the pipeline if the test report shows this test skipped.

## REST load test

Install k6, provision distinct enrolled students with active sessions, then run:

```powershell
$env:BASE_URL="https://staging-api.example"
$env:STUDENT_TOKENS="<token1>,<token2>"
$env:SESSION_IDS="<uuid1>,<uuid2>"
$env:QUESTION_IDS="<questionId1>,<questionId2>"
k6 run load-tests/exam-api.js
```

The workload sends a realistic integrity batch per VU every 12 seconds while driving concurrent
autosaves. Its gates are under 1% failures, integrity p95 below 750 ms, and autosave p95 below
500 ms. Monitor Hikari pending/acquired connections, PostgreSQL CPU/locks, Redis latency, duplicate
event/answer counts, notification queue latency, and tenant-leak audit queries during the run.
These thresholds are artifacts for staging validation; they are not evidence that staging passed.

## LiveKit capacity

REST load results do not establish media capacity. Run a separate LiveKit staging exercise using
the intended region, codecs, resolution, simulcast settings, egress/recording configuration, and
room topology. Increase synthetic publishers/subscribers gradually and observe participant join
p95, reconnects, packet loss, jitter, server CPU/memory, bandwidth, TURN usage, and provider quotas.
Large exams should be split into rooms/shards sized from measured results, with admission limits
and reconnect headroom. Confirm limits with the hosted LiveKit plan or self-hosted node sizing
before announcing a maximum concurrent exam size.
