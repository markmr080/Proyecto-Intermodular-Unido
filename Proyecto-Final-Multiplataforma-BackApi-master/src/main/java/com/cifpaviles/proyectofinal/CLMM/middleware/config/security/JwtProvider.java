package com.cifpaviles.proyectofinal.CLMM.middleware.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;

    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;

    public JwtProvider(@Value("${jwt.secret}") String secretSeed) {
        this.key = Keys.hmacShaKeyFor(secretSeed.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String nombreUsuario) {
        return Jwts.builder()
                .subject(nombreUsuario)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    public String getNombreUsuarioFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- NUEVOS MÉTODOS PARA EL SLIDING SESSION ---

    // 1. Obtener los milisegundos que le quedan de vida al token
    public long getTiempoRestante(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            long fechaExpiracion = claims.getExpiration().getTime();
            long fechaActual = System.currentTimeMillis();
            return fechaExpiracion - fechaActual;
        } catch (Exception e) {
            return 0; // Si hay error, asumimos que caducó
        }
    }
}