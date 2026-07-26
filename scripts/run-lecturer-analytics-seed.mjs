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
  "V13__lecturer_analytics_overview.sql",
);
const sql = fs.readFileSync(sqlPath, "utf8");

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();
  await client.query(sql);

  const countResult = await client.query(`
    SELECT period, COUNT(*)::int AS overview_count
    FROM lecturer_analytics_overviews
    GROUP BY period
    ORDER BY period
  `);

  console.log("Lecturer analytics seed complete.");
  console.table(countResult.rows);
} finally {
  await client.end();
}
