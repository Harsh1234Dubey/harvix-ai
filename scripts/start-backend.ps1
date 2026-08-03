# Starts the Harvix AI backend detached (no console) with the Gemini key.
# Key is read from the User environment variable GEMINI_API_KEY, or from the
# root .env file if present. No secrets are hard-coded here.
$ErrorActionPreference = "Stop"

$key = [Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'User')
if (-not $key -and (Test-Path "$PSScriptRoot\..\.env")) {
  $line = Get-Content "$PSScriptRoot\..\.env" | Where-Object { $_ -match '^GEMINI_API_KEY=' } | Select-Object -First 1
  if ($line) { $key = $line.Substring('GEMINI_API_KEY='.Length) }
}
if (-not $key) { Write-Warning "GEMINI_API_KEY not found. ATS scoring will use the keyword fallback." }

$env:GEMINI_API_KEY = $key
$jar = "$PSScriptRoot\..\backend\target\interview-ai-backend-1.0.0.jar"
if ($key) { Write-Host "Gemini key: loaded (len $($key.Length))" } else { Write-Host "Gemini key: NOT found - ATS will use KEYWORD_FALLBACK" }
if (-not (Test-Path $jar)) { throw "Jar not found: $jar - build it first with: mvn -DskipTests package" }

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "C:\Program Files\Java\jdk-25.0.3\bin\java.exe"
$psi.Arguments = "-jar `"$jar`""
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
if ($key) { $psi.Environment["GEMINI_API_KEY"] = $key }
$proc = [System.Diagnostics.Process]::Start($psi)
Write-Host "Backend starting (PID $($proc.Id)) on :8080 ..."
Start-Sleep 30
try {
  Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"candidate@interviewai.com","password":"Candidate@123"}' | Out-Null
  Write-Host "OK - backend is responding on :8080"
} catch {
  Write-Host "Not ready yet (still booting?): $($_.Exception.Message)"
}
