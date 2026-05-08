package com.cifpaviles.proyectofinal.CLMM.api.controller;

import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEstadisticasService;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.StatsAgregadasDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para exponer las estadísticas agregadas de los jugadores.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final IEstadisticasService estadisticasService;

    public EstadisticasController(IEstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    /**
     * Obtiene las estadísticas agregadas de un jugador dado su username.
     *
     * @param username El nombre de usuario del jugador
     * @return Las estadísticas agregadas (StatsAgregadasDTO)
     */
    @GetMapping("/jugador/{username}")
    public ResponseEntity<StatsAgregadasDTO> getEstadisticasJugador(@PathVariable String username) {
        StatsAgregadasDTO stats = estadisticasService.getStatsAgregadas(username);
        return ResponseEntity.ok(stats);
    }
}
