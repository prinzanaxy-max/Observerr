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

const sqlPath = path.join(__dirname, "seed-lecturer-students-manual.sql");
const sql = fs.readFileSync(sqlPath, "utf8");

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();
  await client.query(sql);

  const roster = await client.query(`
    SELECT u.institutional_id, u.first_name, u.last_name, lc.course_code,
           COUNT(sca.id)::int AS exams_taken,
           ROUND(AVG(sca.integrity_score))::int AS avg_integrity
    FROM lecturer_courses lc
    JOIN student_completed_assessments sca ON sca.course_code = lc.course_code
    JOIN users u ON u.id = sca.student_id
    GROUP BY u.institutional_id, u.first_name, u.last_name, lc.course_code
    ORDER BY u.last_name
  `);

  const sessions = await client.query(`
    SELECT COUNT(*)::int AS session_count FROM proctoring_sessions
  `);

  console.log("Lecturer student seed complete.");
  console.log("Proctoring sessions:", sessions.rows[0].session_count);
  console.table(roster.rows);
} finally {
  await client.end();
}
