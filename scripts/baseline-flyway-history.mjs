import pg from "pg";
import { resolveConnectionString } from "./db-connection.mjs";

/**
 * Neon was populated by manual SQL seeds, so flyway_schema_history is absent.
 * After applying V16/V17 by hand, baseline Flyway at 17 so Spring Boot won't
 * re-run V1–V17 against already-existing tables.
 */
const BASELINE_VERSION = process.env.FLYWAY_BASELINE_VERSION || "17";

const { Client } = pg;
const connectionString = resolveConnectionString();
if (!connectionString) {
  console.error("DATABASE_URL or SPRING_DATASOURCE_* env vars are required");
  process.exit(1);
}

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();
  await client.query("BEGIN");

  await client.query(`
    CREATE TABLE IF NOT EXISTS flyway_schema_history (
      installed_rank INT NOT NULL,
      version VARCHAR(50),
      description VARCHAR(200) NOT NULL,
      type VARCHAR(20) NOT NULL,
      script VARCHAR(1000) NOT NULL,
      checksum INT,
      installed_by VARCHAR(100) NOT NULL,
      installed_on TIMESTAMP NOT NULL DEFAULT now(),
      execution_time INT NOT NULL,
      success BOOLEAN NOT NULL,
      CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank)
    )
  `);

  const existing = await client.query(
    `SELECT version, type FROM flyway_schema_history ORDER BY installed_rank`,
  );
  if (existing.rowCount > 0) {
    console.log("flyway_schema_history already has rows:");
    for (const row of existing.rows) {
      console.log(`  V${row.version ?? "null"} type=${row.type}`);
    }
    await client.query("ROLLBACK");
    process.exit(0);
  }

  await client.query(
    `
    INSERT INTO flyway_schema_history (
      installed_rank, version, description, type, script,
      checksum, installed_by, installed_on, execution_time, success
    ) VALUES (
      1, $1, '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>',
      NULL, current_user, NOW(), 0, TRUE
    )
  `,
    [BASELINE_VERSION],
  );

  await client.query("COMMIT");
  console.log(`Baselines Flyway history at version ${BASELINE_VERSION}.`);
} catch (error) {
  await client.query("ROLLBACK").catch(() => {});
  throw error;
} finally {
  await client.end();
}
