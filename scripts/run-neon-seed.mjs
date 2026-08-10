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

const sqlPath = path.join(__dirname, "seed-student-results-manual.sql");
const sql = fs.readFileSync(sqlPath, "utf8");

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();
  await client.query(sql);

  const countResult = await client.query(`
    SELECT COUNT(*)::int AS assessment_count
    FROM student_completed_assessments sca
    JOIN users u ON u.id = sca.student_id
    WHERE u.institutional_id = 'STU-12345'
  `);

  const previewResult = await client.query(`
    SELECT sca.course_name, sca.course_code, sca.integrity_score, sca.status
    FROM student_completed_assessments sca
    JOIN users u ON u.id = sca.student_id
    WHERE u.institutional_id = 'STU-12345'
    ORDER BY sca.taken_date DESC
    LIMIT 5
  `);

  console.log("Seed complete.");
  console.log("Assessment count:", countResult.rows[0].assessment_count);
  console.log("Sample rows:");
  console.table(previewResult.rows);
} finally {
  await client.end();
}
