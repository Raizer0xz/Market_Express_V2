# fix-springdoc.ps1
# Actualiza springdoc a 2.8.8 en los microservicios con HATEOAS

$root = "C:\Users\Raizer\Downloads\Market_Express_V2"

$microservicios = @(
    "MS-carrito",
    "MS-productos",
    "MS-pagos",
    "MS-delivery"
)

foreach ($ms in $microservicios) {
    $pomPath = "$root\$ms\pom.xml"
    
    if (Test-Path $pomPath) {
        $content = Get-Content $pomPath -Raw -Encoding UTF8
        $updated = $content -replace '<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>\s*\r?\n\s*<version>[^<]+</version>', '<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.8.8</version>'
        
        if ($updated -ne $content) {
            Set-Content $pomPath $updated -Encoding UTF8 -NoNewline
            Write-Host "✅ Actualizado: $ms"
        } else {
            Write-Host "⚠️  No se encontró la dependencia en: $ms"
        }
    } else {
        Write-Host "❌ No encontrado: $ms"
    }
}

Write-Host ""
Write-Host "Listo! Ahora compila los 4 microservicios."
