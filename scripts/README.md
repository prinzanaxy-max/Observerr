# Neon seed scripts (optional)

These are **dev/ops helpers only**. They are **not** part of the Spring Boot application, Maven build, Dockerfile, or Railway deploy.

| What runs in production | Technology |
|---|---|
| API server | Java 21, Spring Boot, Maven, Docker |

## One-time setup (only if you run seeds locally)

```bash
cd scripts
npm install
cd ..
```

Dependencies install into `scripts/node_modules/` (gitignored).

## Run a seed

Set `DATABASE_URL` or `SPRING_DATASOURCE_*` (see `db-connection.mjs`), then from repo root:

```bash
node scripts/run-lecturer-analytics-seed.mjs   # V13
node scripts/run-lecturer-dashboard-seed.mjs   # V14
node scripts/verify-analytics-coverage.mjs     # optional check after V13
```

See root `README.md` for the full seed list.

## Without Node

You can apply the same SQL files from `src/main/resources/db/migration/` using the Neon SQL Editor or `psql` — the `.mjs` files are just convenience wrappers.
