package com.cifpaviles.proyectofinal.CLMM.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de seguridad que valida la cabecera X-Internal-Key en todas las peticiones.
 * Solo el Middleware conoce esta clave, por lo que actúa como handshake entre servicios.
 *
 * Flujo:
 *   1. El Middleware envía X-Internal-Key en TODAS sus peticiones (via BackendClient.defaultHeader).
 *   2. Este filtro la compara con la clave almacenada en application.properties.
 *   3. Si coincide → autentica la petición como "middleware_service" y continúa.
 *   4. Si NO coincide → devuelve 401 y corta la cadena.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private final String expectedKey;

    public InternalApiKeyFilter(@Value("${internal.api.key}") String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String receivedKey = request.getHeader("X-Internal-Key");

        if (expectedKey.equals(receivedKey)) {
            // Clave válida: autenticar como "middleware_service" con rol SERVICE
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "middleware_service",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } else {
            // Clave inválida o ausente: cortar aquí con 401
            System.err.println("[API SECURITY] Petición rechazada - X-Internal-Key inválida o ausente. IP: "
                    + request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Unauthorized\",\"message\":\"Acceso denegado: cabecera X-Internal-Key no válida.\"}"
            );
        }
    }
}
