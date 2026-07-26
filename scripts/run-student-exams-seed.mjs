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

const sqlPath = path.join(
  __dirname,
  "..",
  "src",
  "main",
  "resources",
  "db",
  "migration",
  "V12__enroll_demo_student_exams.sql",
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
    SELECT COUNT(*)::int AS enrollment_count
    FROM exam_enrollments en
    JOIN users u ON u.id = en.student_id
    WHERE u.institutional_id = 'STU-12345'
  `);

  console.log("Student exam enrollment seed complete.");
  console.log("Enrollments for STU-12345:", countResult.rows[0].enrollment_count);
} finally {
  await client.end();
}
