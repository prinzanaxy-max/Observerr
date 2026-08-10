/**
 * Backfill first_name / last_name for users who signed up before name fields existed.
 * Derives names from email local-part (jane.doe → Jane Doe) with institutional_id fallback.
 *
 * Usage (from repo root, with DATABASE_URL or SPRING_DATASOURCE_* set):
 *   node scripts/backfill-user-names.mjs
 *   node scripts/backfill-user-names.mjs --dry-run
 */
import pg from "pg";
import { resolveConnectionString } from "./db-connection.mjs";

const { Client } = pg;
const dryRun = process.argv.includes("--dry-run");

const connectionString = resolveConnectionString();
if (!connectionString) {
  console.error("DATABASE_URL or SPRING_DATASOURCE_* env vars are required");
  process.exit(1);
}

function titleCase(value) {
  return value
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(" ");
}

function clip(value, max = 50) {
  return value.length <= max ? value : value.slice(0, max);
}

function deriveNames(email, institutionalId, existingFirst, existingLast) {
  const local = (email ?? "").split("@")[0] ?? "";
  const parts = local
    .replace(/[._+\-]+/g, " ")
    .replace(/\d+/g, " ")
    .trim()
    .split(/\s+/)
    .filter(Boolean);

  let first = (existingFirst ?? "").trim();
  let last = (existingLast ?? "").trim();

  if (!first) {
    first = parts[0] ? titleCase(parts[0]) : "Student";
  }
  if (!last) {
    if (parts.length >= 2) {
      last = titleCase(parts.slice(1).join(" "));
    } else {
      const id = (institutionalId ?? "").replace(/^(STU|LEC)-/i, "").trim();
      last = id ? titleCase(id.replace(/[^a-zA-Z0-9]+/g, " ")) : "User";
    }
  }

  if (!first) first = "Student";
  if (!last) last = "User";

  return { firstName: clip(first), lastName: clip(last) };
}

const client = new Client({
  connectionString,
  ssl: { rejectUnauthorized: false },
});

try {
  await client.connect();

  const { rows } = await client.query(`
    SELECT id, email, institutional_id, first_name, last_name, role
    FROM users
    WHERE first_name IS NULL OR btrim(first_name) = ''
       OR last_name IS NULL OR btrim(last_name) = ''
    ORDER BY id
  `);

  console.log(`Found ${rows.length} user(s) missing names${dryRun ? " (dry-run)" : ""}.`);

  let updated = 0;
  for (const row of rows) {
    const { firstName, lastName } = deriveNames(
      row.email,
      row.institutional_id,
      row.first_name,
      row.last_name,
    );

    console.log(
      `#${row.id} ${row.role} ${row.institutional_id} <${row.email}> → "${firstName} ${lastName}"`,
    );

    if (!dryRun) {
      await client.query(
        `UPDATE users SET first_name = $1, last_name = $2 WHERE id = $3`,
        [firstName, lastName, row.id],
      );
      updated += 1;
    }
  }

  if (dryRun) {
    console.log("Dry-run complete — no rows written.");
  } else {
    console.log(`Updated ${updated} user(s).`);
  }
} catch (error) {
  console.error("Backfill failed:", error.message);
  process.exit(1);
} finally {
  await client.end();
}
