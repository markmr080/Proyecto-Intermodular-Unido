package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.config.security;

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

    /** Nombre del claim privado donde guardamos el hash del fingerprint */
    private static final String CLAIM_FINGERPRINT = "fp";

    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;

    public JwtProvider(@Value("${jwt.secret}") String secretSeed) {
        this.key = Keys.hmacShaKeyFor(secretSeed.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token CON fingerprint incrustado (uso normal: login del middleware).
     * @param nombreUsuario  subject del token
     * @param fingerprint    hash SHA-256 del navegador enviado por el frontend
     */
    public String generarToken(String nombreUsuario, String fingerprint) {
        return Jwts.builder()
                .subject(nombreUsuario)
                .claim(CLAIM_FINGERPRINT, fingerprint)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Genera un token SIN fingerprint (uso interno: recuperación de contraseña).
     * Se mantiene para no romper el flujo de forgot-password.
     */
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

    // --- MÉTODOS DE FINGERPRINTING ---

    /**
     * Extrae el hash del fingerprint guardado en el claim "fp" del token.
     * Devuelve null si el token no lleva fingerprint (tokens de recuperación).
     */
    public String getFingerprintFromToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return claims.get(CLAIM_FINGERPRINT, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Compara el fingerprint del token con el recibido en la cabecera.
     * Si el token no tiene claim fp (tokens internos), se considera válido.
     */
    public boolean validarFingerprint(String token, String fingerprintCabecera) {
        String fpToken = getFingerprintFromToken(token);
        if (fpToken == null) {
            // Token sin fingerprint (p.ej. reset-password): no aplicamos la restricción
            return true;
        }
        return fpToken.equals(fingerprintCabecera);
    }

    // --- SLIDING SESSION ---

    /** Obtiene los milisegundos que le quedan de vida al token. */
    public long getTiempoRestante(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            long fechaExpiracion = claims.getExpiration().getTime();
            long fechaActual = System.currentTimeMillis();
            return fechaExpiracion - fechaActual;
        } catch (Exception e) {
            return 0;
        }
    }
}
