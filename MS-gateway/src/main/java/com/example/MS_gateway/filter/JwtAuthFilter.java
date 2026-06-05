package com.example.MS_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JwtAuthFilter — filtro de Gateway que valida el token JWT en cada request.
 *
 * Si el token es válido, propaga el usuarioId, email y rol como headers
 * para que los MS internos puedan usarlos sin volver a validar.
 *
 * Uso en application.properties:
 *   spring.cloud.gateway.routes[N].filters[0]=JwtAuthFilter
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Token no proporcionado");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String rol = claims.get("rol", String.class);

                // Propagar datos del usuario como headers internos
                // Los MS internos pueden leerlos sin necesidad de validar JWT de nuevo
                var modifiedRequest = exchange.getRequest().mutate()
                        .header("X-Usuario-Id", String.valueOf(claims.get("usuarioId")))
                        .header("X-Usuario-Email", claims.getSubject())
                        .header("X-Usuario-Rol", rol != null ? rol : "")
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (JwtException | IllegalArgumentException e) {
                return unauthorized(exchange, "Token inválido o expirado");
            }
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String mensaje) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        var body = exchange.getResponse().bufferFactory()
                .wrap(("{\"error\": \"" + mensaje + "\"}").getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(body));
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static class Config {}
}