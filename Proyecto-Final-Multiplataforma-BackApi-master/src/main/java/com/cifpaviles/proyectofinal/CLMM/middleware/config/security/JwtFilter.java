package com.cifpaviles.proyectofinal.CLMM.middleware.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Value("${jwt.renewal.ms}")
    private long TIEMPO_RENOVACION_MS;

    public JwtFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String tokenAntiguo = authHeader.substring(7);

            // --- CHIVATOS PARA LA CONSOLA (IntelliJ) ---
            System.out.println("\n--- NUEVA PETICIÓN INTERCEPTADA ---");
            boolean esValido = jwtProvider.validarToken(tokenAntiguo);
            System.out.println("¿ES VÁLIDO EL TOKEN?: " + esValido);
            // --------------------------------------------

            if (esValido) {

                // --- VALIDACIÓN DE FINGERPRINT ---
                String fingerprintCabecera = request.getHeader("X-Fingerprint");
                boolean fingerprintOk = jwtProvider.validarFingerprint(tokenAntiguo, fingerprintCabecera);
                System.out.println("X-Fingerprint recibido: " + fingerprintCabecera);
                System.out.println("¿FINGERPRINT VÁLIDO?: " + fingerprintOk);

                if (!fingerprintOk) {
                    System.out.println("FINGERPRINT NO COINCIDE - Petición bloqueada");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Token fingerprint mismatch. Uso del token desde un navegador no autorizado.\"}"
                    );
                    return; // Cortamos la cadena aquí, sin pasar al siguiente filtro
                }
                // ----------------------------------

                String username = jwtProvider.getNombreUsuarioFromToken(tokenAntiguo);

                // --- LÓGICA DE RENOVACIÓN ---
                long tiempoRestante = jwtProvider.getTiempoRestante(tokenAntiguo);
                System.out.println("TIEMPO RESTANTE (ms): " + tiempoRestante);

                String estadoPrueba = "TOKEN_VALIDO_NORMAL"; // Mensaje por defecto

                if (tiempoRestante < TIEMPO_RENOVACION_MS && tiempoRestante > 0) {
                    System.out.println("¡RENOVANDO EL TOKEN AHORA MISMO!");

                    // Al renovar, propagamos el mismo fingerprint al nuevo token
                    String tokenNuevo = (fingerprintCabecera != null && !fingerprintCabecera.isBlank())
                            ? jwtProvider.generarToken(username, fingerprintCabecera)
                            : jwtProvider.generarToken(username);

                    response.setHeader("Token-Nuevo", tokenNuevo);
                    response.setHeader("Access-Control-Expose-Headers", "Token-Nuevo");

                    estadoPrueba = "TOKEN_RENOVADO_DINAMICAMENTE"; // Cambiamos el mensaje
                }

                // Guardamos el mensaje en la petición para que el GameController pueda leerlo
                request.setAttribute("estado-token", estadoPrueba);
                // ----------------------------------------

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.emptyList());

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}