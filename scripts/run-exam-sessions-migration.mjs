import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import pg from "pg";
import { resolveConnectionString } from "./db-connection.mjs";

const { Client } = pg;
const __dirname = path.dirname(fileURLToPath(import.meta.url));

const connectionString = resolveConnectionString();
if (!connectionString) {
  console.error("DATABASE_URL or SPRING_DATASOURCE_* env vars are required");
  process.exit(1);
}

const sqlPath = path.join(
  __dirname,
  "..",
  "src",
  "main",
  "resources",
  "db",
  "migration",
  "V11__exam_sessions_integrity_events.sql",
);
const sql = fs.readFileSync(sqlPath, "utf8");

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();
  await client.query(sql);

  const tables = await client.query(`
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name IN ('exam_sessions', 'integrity_events')
    ORDER BY table_name
  `);

  console.log("V11 migration complete.");
  console.log("Tables:", tables.rows.map((row) => row.table_name).join(", "));
} finally {
  await client.end();
}
