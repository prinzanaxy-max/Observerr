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

const migrations = [
  "V16__exam_questions_answers_results.sql",
  "V17__operational_notifications_and_exam_blocks.sql",
];

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();

  for (const file of migrations) {
    const sqlPath = path.join(__dirname, "..", "src", "main", "resources", "db", "migration", file);
    const sql = fs.readFileSync(sqlPath, "utf8");
    console.log(`Applying ${file}...`);
    await client.query("BEGIN");
    try {
      await client.query(sql);
      await client.query("COMMIT");
      console.log(`OK ${file}`);
    } catch (error) {
      await client.query("ROLLBACK");
      if (String(error.message).includes("already exists")) {
        console.log(`SKIP ${file} (objects already exist): ${error.message}`);
        continue;
      }
      throw error;
    }
  }

  const tables = await client.query(`
    SELECT table_name
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name IN (
        'exam_questions', 'exam_question_options', 'exam_answers', 'exam_results',
        'notifications', 'notification_preferences', 'exam_student_blocks'
      )
    ORDER BY table_name
  `);
  console.log("Tables present:", tables.rows.map((row) => row.table_name).join(", "));
} finally {
  await client.end();
}
