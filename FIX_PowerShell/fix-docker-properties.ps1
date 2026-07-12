# fix-docker-properties.ps1
# Ejecutar desde la carpeta raiz del proyecto Market_Express_V2
# Uso: .\fix-docker-properties.ps1

$micros = @(
    @{ carpeta = "MS-pedidos";          bd = "db_pedidos" },
    @{ carpeta = "MS-usuarios";         bd = "db_usuarios" },
    @{ carpeta = "MS-productos";        bd = "db_productos" },
    @{ carpeta = "MS-carrito";          bd = "db_carrito" },
    @{ carpeta = "MS-sucursales";       bd = "db_sucursales" },
    @{ carpeta = "MS-pagos";            bd = "db_pagos" },
    @{ carpeta = "MS-inventario";       bd = "db_inventario" },
    @{ carpeta = "MS-delivery";         bd = "db_delivery" },
    @{ carpeta = "MS_gateway_security"; bd = "db_seguridad" }
)

foreach ($micro in $micros) {
    $path = ".\$($micro.carpeta)\src\main\resources\application-docker.properties"

    if (Test-Path $path) {
        $contenido = Get-Content $path -Raw

        # Fix 1: Reemplazar host.docker.internal por mysql (servicio Docker)
        $contenido = $contenido -replace "jdbc:mysql://host\.docker\.internal:\d+/[^`n\r]*", "jdbc:mysql://mysql:3306/$($micro.bd)?createDatabaseIfNotExist=true"

        # Fix 2: Asegurar usuario root y password root
        $contenido = $contenido -replace "spring\.datasource\.username=.*", "spring.datasource.username=root"
        $contenido = $contenido -replace "spring\.datasource\.password=.*", "spring.datasource.password=root"

        Set-Content $path $contenido -NoNewline
        Write-Host "✅ $($micro.carpeta) -> $($micro.bd)" -ForegroundColor Green
    } else {
        Write-Host "❌ No encontrado: $path" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "¡Listo! Todos los application-docker.properties actualizados." -ForegroundColor Cyan
