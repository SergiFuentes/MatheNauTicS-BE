# run-local.ps1
# Loads .env into environment variables and runs the Spring Boot app.
# Bypasses Spring's [.properties] parser, which mangles certain characters.

$ErrorActionPreference = "Stop"

if (-not (Test-Path .env)) {
    Write-Error ".env not found in $(Get-Location). Create it with DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET."
    exit 1
}

$loaded = 0
Get-Content .env | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { return }
    $idx = $line.IndexOf('=')
    if ($idx -lt 1) { return }
    $name  = $line.Substring(0, $idx).Trim()
    $value = $line.Substring($idx + 1)
    Set-Item -Path "env:$name" -Value $value
    $loaded++
}

Write-Host "Loaded $loaded vars from .env" -ForegroundColor Green

.\gradlew bootRun
