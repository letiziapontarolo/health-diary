$ErrorActionPreference = "Stop"

# Locate the project directories
$projectRoot = $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"
$backendDir = Join-Path $projectRoot "backend"

# Check required files
if (-not (Test-Path $envFile)) {
    Write-Error "Missing .env file."
    exit 1
}

if (-not (Test-Path (Join-Path $backendDir "mvnw.cmd"))) {
    Write-Error "Missing Maven wrapper in backend directory."
    exit 1
}

# Load PostgreSQL password
$passwordLine = Get-Content $envFile |
    Where-Object { $_ -match '^POSTGRES_PASSWORD=' } |
    Select-Object -First 1

if (-not $passwordLine) {
    Write-Error "POSTGRES_PASSWORD is missing from .env."
    exit 1
}

$env:POSTGRES_PASSWORD = $passwordLine.Substring(
    $passwordLine.IndexOf("=") + 1
)

Remove-Variable passwordLine

# Check Docker container
$containerName = "health-diary-postgres"

$containerStatus = docker inspect `
    --format '{{.State.Running}}' $containerName 2>$null

if ($LASTEXITCODE -ne 0 -or $containerStatus -ne "true") {
    Write-Error "PostgreSQL container is not running. Start it in Docker Desktop."
    exit 1
}

# Start Spring Boot
Write-Host "Starting Health Diary backend..." -ForegroundColor Cyan

Set-Location $backendDir

try {
    .\mvnw.cmd spring-boot:run
}
finally {
    Remove-Item Env:POSTGRES_PASSWORD -ErrorAction SilentlyContinue
}
