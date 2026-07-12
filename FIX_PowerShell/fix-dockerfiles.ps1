# fix-dockerfiles.ps1
# Ejecutar desde la carpeta raiz del proyecto Market_Express_V2
# Uso: Unblock-File .\fix-dockerfiles.ps1; .\fix-dockerfiles.ps1

$micros = @(
    @{ carpeta = "MS-pedidos";          puerto = "8088" },
    @{ carpeta = "MS-usuarios";         puerto = "8084" },
    @{ carpeta = "MS-productos";        puerto = "8085" },
    @{ carpeta = "MS-carrito";          puerto = "8086" },
    @{ carpeta = "MS-sucursales";       puerto = "8087" },
    @{ carpeta = "MS-pagos";            puerto = "9091" },
    @{ carpeta = "MS-inventario";       puerto = "8089" },
    @{ carpeta = "MS-delivery";         puerto = "9090" },
    @{ carpeta = "MS_gateway_security"; puerto = "8082" },
    @{ carpeta = "MS-gateway";          puerto = "5050" },
    @{ carpeta = "MS-eureka-server";    puerto = "8761" }
)

foreach ($micro in $micros) {
    $path = ".\$($micro.carpeta)\dockerfile"

    if (Test-Path $path) {
        $contenido = @"
# Etapa 1: compilacion con Maven
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests 2>/dev/null || mvn clean package -DskipTests

# Etapa 2: imagen final solo con JRE
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE $($micro.puerto)
ENTRYPOINT ["java","-jar","app.jar"]
"@
        Set-Content $path $contenido -NoNewline
        Write-Host "✅ $($micro.carpeta) -> puerto $($micro.puerto)" -ForegroundColor Green
    } else {
        Write-Host "❌ No encontrado: $path" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Listo! Todos los Dockerfiles actualizados con multi-stage build." -ForegroundColor Cyan
