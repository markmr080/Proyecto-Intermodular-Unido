package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    @GetMapping("/estado")
    public ResponseEntity<String> estadoPartida(HttpServletRequest request) {
        // Leemos lo que el filtro nos ha dejado escrito
        String estadoToken = (String) request.getAttribute("estado-token");

        // Si no hay token (ej. ruta pública), evitamos que sea null
        if (estadoToken == null) estadoToken = "SIN_TOKEN_NO_HAY_FIESTA";

        return ResponseEntity.ok("Respuesta del servidor: [" + estadoToken + "] | Estás dentro de la zona segura.");
    }
}
