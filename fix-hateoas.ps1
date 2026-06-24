# fix-hateoas.ps1
# Agrega springdoc.enable-hateoas=false a los application.properties

$root = "C:\Users\Raizer\Downloads\Market_Express_V2"

$microservicios = @(
    "MS-carrito",
    "MS-productos",
    "MS-pagos",
    "MS-delivery",
    "MS-sucursales"
)

foreach ($ms in $microservicios) {
    $propPath = "$root\$ms\src\main\resources\application.properties"
    
    if (Test-Path $propPath) {
        $content = Get-Content $propPath -Raw -Encoding UTF8
        
        if ($content -notmatch "springdoc.enable-hateoas") {
            $content = $content.TrimEnd()
            $content += "`nspringdoc.enable-hateoas=false`n"
            Set-Content $propPath $content -Encoding UTF8 -NoNewline
            Write-Host "✅ Arreglado: $ms"
        } else {
            Write-Host "⏭️  Ya tiene la propiedad: $ms"
        }
    } else {
        Write-Host "❌ No encontrado: $ms"
    }
}

Write-Host ""
Write-Host "Listo! Ahora compila los 5 microservicios."
