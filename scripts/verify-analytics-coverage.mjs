import pg from "pg";
import { resolveConnectionString } from "./db-connection.mjs";

const { Client } = pg;
const client = new Client({
  connectionString: resolveConnectionString(),
  ssl: { rejectUnauthorized: false },
});

await client.connect();

const lecturers = await client.query(`
  SELECT id, institutional_id FROM users WHERE role = 'LECTURER' ORDER BY id
`);

const missing = await client.query(`
  SELECT u.id, u.institutional_id
  FROM users u
  WHERE u.role = 'LECTURER'
    AND NOT EXISTS (
      SELECT 1 FROM lecturer_analytics_overviews o
      WHERE o.lecturer_id = u.id AND o.period = '7D'
    )
  ORDER BY u.id
`);

console.log("lecturers:", lecturers.rows.length);
console.log("institutional_ids:", lecturers.rows.map((r) => r.institutional_id).join(", "));
console.log("missing_7d:", missing.rows.length);
if (missing.rows.length) {
  console.table(missing.rows);
}

await client.end();
