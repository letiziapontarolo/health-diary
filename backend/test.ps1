
# Health Diary - Backend Test Runner

$ErrorActionPreference = "Stop"

# Always run from the backend directory.
Set-Location $PSScriptRoot

$logDirectory = Join-Path $PSScriptRoot "target\test-logs"
$logFile = Join-Path $logDirectory "latest.log"

New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "       HEALTH DIARY - BACKEND TESTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Running tests... Please wait." -ForegroundColor Yellow
Write-Host ""

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

# Run Maven and save all output without printing it.
$process = Start-Process `
    -FilePath (Join-Path $PSScriptRoot "mvnw.cmd") `
    -ArgumentList "test" `
    -WorkingDirectory $PSScriptRoot `
    -RedirectStandardOutput $logFile `
    -RedirectStandardError (Join-Path $logDirectory "stderr.log") `
    -NoNewWindow `
    -Wait `
    -PassThru

$stopwatch.Stop()

$exitCode = $process.ExitCode

# Read the final Maven summary.
$log = Get-Content $logFile -Raw

$summary = [regex]::Matches(
    $log,
    'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)'
)

# Maven prints per-class results and a final total.
if ($summary.Count -gt 0) {
    $last = $summary[$summary.Count - 1]

    $tests = [int]$last.Groups[1].Value
    $failures = [int]$last.Groups[2].Value
    $errors = [int]$last.Groups[3].Value
    $skipped = [int]$last.Groups[4].Value
    $passed = $tests - $failures - $errors - $skipped

    Write-Host "----------------------------------------"
    Write-Host "TEST RESULTS"
    Write-Host "----------------------------------------"
    Write-Host "Tests:     $tests"
    Write-Host "Passed:    $passed" -ForegroundColor Green
    Write-Host "Failures:  $failures"
    Write-Host "Errors:    $errors"
    Write-Host "Skipped:   $skipped"
    Write-Host ""
}
else {
    Write-Host "Test summary unavailable. Check the full log." -ForegroundColor Yellow
    Write-Host ""
}

if ($exitCode -eq 0) {
    Write-Host "BUILD SUCCESS" -ForegroundColor Green
}
else {
    Write-Host "BUILD FAILURE" -ForegroundColor Red

    # Show Maven's error lines.
    Get-Content $logFile |
        Select-String '^\[ERROR\]' |
        Select-Object -Last 15 |
        ForEach-Object {
            Write-Host $_.Line -ForegroundColor Red
        }
}

Write-Host ("Duration:  {0:N1} seconds" -f $stopwatch.Elapsed.TotalSeconds)
Write-Host "Full log:  target/test-logs/latest.log" -ForegroundColor DarkGray
Write-Host ""

if ($exitCode -ne 0) {
    exit $exitCode
}
