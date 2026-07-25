import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import pg from "pg";

const { Client } = pg;
const __dirname = path.dirname(fileURLToPath(import.meta.url));

const connectionString = process.env.DATABASE_URL;
if (!connectionString) {
  console.error("DATABASE_URL is required");
  process.exit(1);
}

const sqlPath = path.join(__dirname, "seed-lecturer-exams-manual.sql");
const sql = fs.readFileSync(sqlPath, "utf8");

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();
  await client.query(sql);

  const exams = await client.query(`
    SELECT e.id, e.title, e.course_code, e.status, e.start_time, e.enrolled_count, e.active_flags_count
    FROM exams e
    JOIN users u ON u.id = e.lecturer_id
    WHERE u.role = 'LECTURER'
    ORDER BY e.start_time DESC
  `);

  console.log("Lecturer exams seed complete.");
  console.table(exams.rows);
} finally {
  await client.end();
}
