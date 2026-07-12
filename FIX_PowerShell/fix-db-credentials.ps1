# fix-db-credentials.ps1
# Agrega usuario y contraseña MySQL a todos los application-docker.properties

$root = "C:\Users\Raizer\Downloads\Market_Express_V2"

$microservicios = @(
    "MS-gateway",
    "MS_gateway_security",
    "MS-usuarios",
    "MS-productos",
    "MS-carrito",
    "MS-sucursales",
    "MS-pedidos",
    "MS-inventario",
    "MS-pagos",
    "MS-delivery"
)

foreach ($ms in $microservicios) {
    $propPath = "$root\$ms\src\main\resources\application-docker.properties"
    
    if (Test-Path $propPath) {
        $content = Get-Content $propPath -Raw -Encoding UTF8

        if ($content -notmatch "spring.datasource.username") {
            $content = $content.TrimEnd()
            $content += "`nspring.datasource.username=root`nspring.datasource.password=root`n"
            Set-Content $propPath $content -Encoding UTF8 -NoNewline
            Write-Host "✅ Credenciales agregadas: $ms"
        } else {
            Write-Host "⏭️  Ya tiene credenciales: $ms"
        }
    } else {
        Write-Host "⏭️  Sin BD (no aplica): $ms"
    }
}

Write-Host ""
Write-Host "Listo! Ahora vuelve a compilar y levantar docker-compose."
