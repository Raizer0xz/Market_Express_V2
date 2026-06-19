package com.example.MS_gateway_security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtService — crea y valida tokens JWT.
 *
 * El token contiene:
 *   sub       → email del usuario
 *   usuarioId → ID en ms-usuarios
 *   rol       → CLIENTE | REPARTIDOR | ADMIN_SUCURSAL
 *   iat       → fecha de emision
 *   exp       → expira en 24h por defecto
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String email, Long usuarioId, String rol) {
        return Jwts.builder()
                .setSubject(email)
                .claim("usuarioId", usuarioId)
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey(), SignatureAlgorithm.HS256) // HS256 explícito, evita WeakKeyException
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extraerEmail(String token)     { return extraerClaims(token).getSubject(); }
    public Long   extraerUsuarioId(String token) { return extraerClaims(token).get("usuarioId", Long.class); }
    public String extraerRol(String token)       { return extraerClaims(token).get("rol", String.class); }

    public boolean esTokenValido(String token) {
        try {
            return extraerClaims(token).getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}