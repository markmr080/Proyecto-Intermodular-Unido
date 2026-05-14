package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.controller;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.StatsAgregadasDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final BackendClient backendClient;

    public EstadisticasController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping("/jugador/{username}")
    public ResponseEntity<StatsAgregadasDTO> getEstadisticasJugador(@PathVariable String username) {
        StatsAgregadasDTO stats = backendClient.getEstadisticasJugador(username);
        return ResponseEntity.ok(stats);
    }
}
