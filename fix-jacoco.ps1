# fix-jacoco.ps1
# Ejecutar desde la carpeta raiz del proyecto Market_Express_V2
# Uso: Unblock-File .\fix-jacoco.ps1; .\fix-jacoco.ps1

$micros = @(
    "MS-pedidos", "MS-usuarios", "MS-productos", "MS-carrito",
    "MS-sucursales", "MS-pagos", "MS-inventario", "MS-delivery",
    "MS_gateway_security"
)

$jacoco = @"

            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>check</id>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>BUNDLE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.70</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
"@

foreach ($micro in $micros) {
    $path = ".\$micro\pom.xml"

    if (Test-Path $path) {
        $contenido = Get-Content $path -Raw

        # Verificar si ya tiene JaCoCo
        if ($contenido -match "jacoco") {
            Write-Host "⚠️  $micro ya tiene JaCoCo" -ForegroundColor Yellow
            continue
        }

        # Insertar JaCoCo antes del cierre de </plugins>
        $contenido = $contenido -replace "</plugins>", "$jacoco`n            </plugins>"
        Set-Content $path $contenido -NoNewline
        Write-Host "✅ $micro -> JaCoCo agregado" -ForegroundColor Green
    } else {
        Write-Host "❌ No encontrado: $path" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Listo! Para ver el reporte ejecuta en cada micro:" -ForegroundColor Cyan
Write-Host "  .\mvnw.cmd test" -ForegroundColor White
Write-Host "  El reporte queda en: target/site/jacoco/index.html" -ForegroundColor White
