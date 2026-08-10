# Requires: railway login (npx @railway/cli login)
# Reads VAPID_* from Observer-Backend/.env and sets them on the linked Railway service.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$envFile = Join-Path $root '.env'
if (-not (Test-Path $envFile)) {
  Write-Error "Missing $envFile — copy .env.example to .env and fill VAPID keys first."
}

$vars = @{}
Get-Content $envFile | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
  $parts = $_ -split '=', 2
  if ($parts.Length -eq 2) { $vars[$parts[0].Trim()] = $parts[1].Trim() }
}

foreach ($key in @('VAPID_PUBLIC_KEY', 'VAPID_PRIVATE_KEY', 'VAPID_SUBJECT')) {
  if (-not $vars[$key]) { Write-Error "Missing $key in .env" }
}

Write-Host "Setting Railway variables from .env ..."
npx --yes @railway/cli variables set `
  "VAPID_PUBLIC_KEY=$($vars.VAPID_PUBLIC_KEY)" `
  "VAPID_PRIVATE_KEY=$($vars.VAPID_PRIVATE_KEY)" `
  "VAPID_SUBJECT=$($vars.VAPID_SUBJECT)"

Write-Host "Done. Redeploy the API, then verify GET /ready shows webpush: CONFIGURED."
