param(
  [string]$Distro = "Ubuntu",
  [string]$DbTarget = "/data/install/db",
  [string]$ModelEndpoint = "https://llm-fmljnd0r34k4pinm.cn-beijing.maas.aliyuncs.com/compatible-mode/v1/chat/completions",
  [string]$ModelName = "qwen3.7-max"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Logs = Join-Path $Root "logs"
New-Item -ItemType Directory -Force -Path $Logs | Out-Null

if (-not $env:DITU_DEFAULT_MODEL_API_KEY) {
  $secure = Read-Host "Input model Authorization value" -AsSecureString
  $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  )
  $env:DITU_DEFAULT_MODEL_API_KEY = $plain
}

function Invoke-WslChecked {
  param([string]$Command)
  wsl -d $Distro -- bash -lc $Command
  if ($LASTEXITCODE -ne 0) {
    throw "WSL command failed: $Command"
  }
}

function ConvertTo-WslSingleQuoted {
  param([string]$Value)
  if ($Value.Contains("'")) {
    throw "Single quote is not supported in this WSL shell argument."
  }
  return "'" + $Value + "'"
}

Write-Host "Installing WSL DB scripts..."
& powershell -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "install-wsl-db.ps1") -Distro $Distro -Target $DbTarget
if ($LASTEXITCODE -ne 0) {
  throw "Failed to install WSL DB scripts."
}

Write-Host "Starting WSL PostgreSQL/pgvector..."
$dbStart = "cd $(ConvertTo-WslSingleQuoted $DbTarget) && ./start.sh"
wsl -d $Distro -- bash -lc $dbStart
if ($LASTEXITCODE -ne 0) {
  $secure = Read-Host "Input WSL sudo password for Docker service" -AsSecureString
  $sudoPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  )
  $dbStartWithPassword = "cd $(ConvertTo-WslSingleQuoted $DbTarget) && SUDO_PASSWORD=$(ConvertTo-WslSingleQuoted $sudoPassword) ./start.sh"
  Invoke-WslChecked $dbStartWithPassword
}

$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/ditu"
$env:SPRING_DATASOURCE_USERNAME = "ditu"
$env:SPRING_DATASOURCE_PASSWORD = "ditu"
$env:DITU_AGENT_RUNTIME = "compatible"
$env:DITU_DEFAULT_MODEL_BASE_URL = $ModelEndpoint
$env:DITU_DEFAULT_MODEL_NAME = $ModelName
$env:DITU_DEFAULT_MODEL_AUTH_TYPE = "API_KEY"
$env:DITU_AUTH_TOKEN_SECRET = "local-wsl-token-secret-change-me"

Write-Host "Starting backend Spring Boot..."
Start-Process -FilePath "mvn.cmd" `
  -ArgumentList "spring-boot:run" `
  -WorkingDirectory (Join-Path $Root "backend") `
  -WindowStyle Hidden `
  -RedirectStandardOutput (Join-Path $Logs "backend.out.log") `
  -RedirectStandardError (Join-Path $Logs "backend.err.log")

Write-Host "Starting frontend Vite apps..."
$frontends = @(
  @{ Name = "h5"; Port = 5173; Dir = Join-Path $Root "frontend/apps/h5" },
  @{ Name = "admin"; Port = 5174; Dir = Join-Path $Root "frontend/apps/admin" },
  @{ Name = "miniprogram"; Port = 5175; Dir = Join-Path $Root "frontend/apps/miniprogram" }
)
foreach ($app in $frontends) {
  Start-Process -FilePath "pnpm.cmd" `
    -ArgumentList "dev" `
    -WorkingDirectory $app.Dir `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $Logs "$($app.Name).out.log") `
    -RedirectStandardError (Join-Path $Logs "$($app.Name).err.log")
}

Write-Host "Waiting for backend health..."
for ($i = 0; $i -lt 30; $i++) {
  try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2
    if ($health.status -eq "UP") {
      break
    }
  } catch {
    Start-Sleep -Seconds 2
  }
}

Write-Host ""
Write-Host "Started:"
Write-Host "Backend health: http://localhost:8080/actuator/health"
Write-Host "H5:             http://localhost:5173"
Write-Host "Admin:          http://localhost:5174"
Write-Host "Mini preview:   http://localhost:5175"
Write-Host "Logs:           $Logs"
