package com.cifpaviles.proyectofinal.CLMM.api.controller;

import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEstadisticasService;
import com.cifpaviles.proyectofinal.CLMM.api.model.dto.StatsAgregadasDTO;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final IEstadisticasService estadisticasService;
    private final UsuarioRepository usuarioRepository;
    private final PersonajeRepository personajeRepository;

    public EstadisticasController(IEstadisticasService estadisticasService,
                                   UsuarioRepository usuarioRepository,
                                   PersonajeRepository personajeRepository) {
        this.estadisticasService = estadisticasService;
        this.usuarioRepository = usuarioRepository;
        this.personajeRepository = personajeRepository;
    }

    @GetMapping("/jugador/{username}")
    public ResponseEntity<StatsAgregadasDTO> getEstadisticasJugador(@PathVariable String username) {
        StatsAgregadasDTO stats = estadisticasService.getStatsAgregadas(username);
        return ResponseEntity.ok(stats);
    }

    /**
     * Endpoint interno para que el Middleware guarde las stats al finalizar una partida.
     * No requiere autenticación JWT (comunicación entre microservicios en red privada).
     * Body: { username, personajeNombre, hitsAcertados, hitsFallados, barcosHundidos, ganador }
     */
    @PostMapping("/guardar")
    public ResponseEntity<?> guardarStats(@RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            String personajeNombre = (String) body.getOrDefault("personajeNombre", "");
            int hitsAcertados   = ((Number) body.getOrDefault("hitsAcertados", 0)).intValue();
            int hitsFallados    = ((Number) body.getOrDefault("hitsFallados",  0)).intValue();
            int barcosHundidos  = ((Number) body.getOrDefault("barcosHundidos", 0)).intValue();
            boolean ganador     = Boolean.TRUE.equals(body.get("ganador"));

            Long idUsuario = usuarioRepository.findByUsername(username)
                .map(u -> u.getId()).orElse(null);
            Long idPersonaje = personajeRepository.findByNombre(personajeNombre)
                .map(p -> p.getId()).orElse(null);

            estadisticasService.guardarStatsPartida(null, idUsuario, idPersonaje,
                hitsAcertados, hitsFallados, barcosHundidos, username, ganador);

            return ResponseEntity.ok(Map.of("message", "Stats guardadas para " + username));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
