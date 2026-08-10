import pg from "pg";
import { resolveConnectionString } from "./db-connection.mjs";

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
  const exists = await client.query(`
    SELECT EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'
    ) AS present
  `);
  if (!exists.rows[0].present) {
    console.log("flyway_schema_history: missing");
    process.exit(0);
  }

  const rows = await client.query(`
    SELECT installed_rank, version, description, success, installed_on
    FROM flyway_schema_history
    ORDER BY installed_rank
  `);
  for (const row of rows.rows) {
    console.log(
      `rank=${row.installed_rank} V${row.version ?? "null"} ${row.description} success=${row.success}`,
    );
  }
  console.log(`total=${rows.rowCount}`);
} finally {
  await client.end();
}
