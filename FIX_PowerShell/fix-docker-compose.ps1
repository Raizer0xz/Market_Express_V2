# fix-docker-compose.ps1
# Ejecutar desde la carpeta raiz del proyecto Market_Express_V2
# Uso: .\fix-docker-compose.ps1

$path = ".\docker-compose.yml"

if (Test-Path $path) {
    $contenido = Get-Content $path -Raw

    # Quitar todas las lineas de extra_hosts
    $contenido = $contenido -replace "    extra_hosts:\r?\n      - ""host\.docker\.internal:host-gateway""\r?\n", ""

    Set-Content $path $contenido -NoNewline
    Write-Host "✅ docker-compose.yml actualizado — extra_hosts eliminados" -ForegroundColor Green
} else {
    Write-Host "❌ No encontrado: $path" -ForegroundColor Red
}

Write-Host ""
Write-Host "¡Listo! Ahora los micros conectan a MySQL via red Docker." -ForegroundColor Cyan
