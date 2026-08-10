export function resolveConnectionString() {
  if (process.env.DATABASE_URL) {
    return process.env.DATABASE_URL;
  }

  const jdbcUrl = process.env.SPRING_DATASOURCE_URL;
  const username = process.env.SPRING_DATASOURCE_USERNAME;
  const password = process.env.SPRING_DATASOURCE_PASSWORD;

  if (!jdbcUrl || !username || !password) {
    return null;
  }

  const normalized = jdbcUrl.replace(/^jdbc:postgresql:/, "postgresql:");
  const url = new URL(normalized);
  url.username = encodeURIComponent(username);
  url.password = encodeURIComponent(password);
  return url.toString();
}
