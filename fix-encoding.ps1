# fix-encoding.ps1
# Agrega ISO-8859-1 encoding a todos los pom.xml de Market Express V2

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
    $pomPath = "$root\$ms\pom.xml"
    
    if (Test-Path $pomPath) {
        $content = Get-Content $pomPath -Raw -Encoding UTF8
        
        if ($content -notmatch "project.build.sourceEncoding") {
            $content = $content -replace "<java.version>21</java.version>", "<java.version>21</java.version>`n        <project.build.sourceEncoding>ISO-8859-1</project.build.sourceEncoding>"
            Set-Content $pomPath $content -Encoding UTF8 -NoNewline
            Write-Host "✅ Arreglado: $ms"
        } else {
            Write-Host "⏭️  Ya tiene encoding: $ms"
        }
    } else {
        Write-Host "❌ No encontrado: $ms"
    }
}

Write-Host ""
Write-Host "Listo! Ahora puedes compilar todos los microservicios."
