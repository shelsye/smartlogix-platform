$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

if (-not (Test-Path ".\docker-compose.yml")) {
    throw "No se encontró docker-compose.yml en $root"
}

try {
    $serverVersion = docker version --format '{{.Server.Version}}' 2>$null
    if (-not $serverVersion) { throw "Sin respuesta del servidor Docker" }
} catch {
    throw "Docker Desktop no tiene activo el motor Linux. Abre Docker Desktop, espera 'Engine running' y vuelve a ejecutar este script."
}

docker compose config | Out-Null
docker compose up --build -d
docker compose ps

Write-Host ""
Write-Host "SmartLogix iniciado: http://localhost:3000" -ForegroundColor Green
Write-Host "API Gateway: http://localhost:8080"
Write-Host "Eureka: http://localhost:8762"
Write-Host "Admin: admin@smartlogix.com / admin1"
