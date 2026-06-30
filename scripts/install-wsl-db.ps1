param(
  [string]$Distro = "Ubuntu",
  [string]$Target = "/data/install/db"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Source = Join-Path $Root "deploy/wsl-db"
$WslSource = (wsl -d $Distro -- wslpath -a "$Source").Trim()

wsl -d $Distro -- bash -lc "mkdir -p '$Target' && cp -r '$WslSource'/.[!.]* '$WslSource'/* '$Target'/ 2>/dev/null || true && chmod +x '$Target'/*.sh"
if ($LASTEXITCODE -ne 0) {
  throw "Failed to install WSL database scripts"
}

Write-Host "Installed to WSL: $Target"
Write-Host "Start command: wsl -d $Distro -- bash -lc 'cd $Target && ./start.sh'"
