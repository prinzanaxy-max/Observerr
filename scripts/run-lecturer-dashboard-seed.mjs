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
  "V14__lecturer_dashboard_demo_sessions.sql",
);
const sql = fs.readFileSync(sqlPath, "utf8");

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();
  await client.query(sql);

  const result = await client.query(`
    SELECT COUNT(*)::int AS sessions
    FROM exam_sessions es
    JOIN exams e ON e.id = es.exam_id
    JOIN users l ON l.id = e.lecturer_id
    WHERE l.institutional_id = 'STU-67890'
  `);

  console.log("Lecturer dashboard demo seed complete.");
  console.log("exam_sessions for demo lecturer:", result.rows[0].sessions);
} finally {
  await client.end();
}
