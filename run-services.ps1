# Conservado por compatibilidad con la estructura original.
# La plataforma completa debe iniciarse con Docker para mantener Eureka,
# redes internas, bases persistentes y Nginx sincronizados.
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
& ".\run-docker.ps1"
