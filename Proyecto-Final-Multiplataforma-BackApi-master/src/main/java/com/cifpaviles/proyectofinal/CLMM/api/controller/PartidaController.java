package com.cifpaviles.proyectofinal.CLMM.api.controller;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.EstadoPartida;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository;
import com.cifpaviles.proyectofinal.CLMM.api.model.repository.PartidaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para consultar partidas persistidas en MySQL.
 * 
 * NOTA: La creación/gestión de partidas durante el juego se realiza
 * vía WebSockets (GameSocketController). Este controller solo expone
 * consultas de historial y estado de partidas ya guardadas.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/partidas")
public class PartidaController {

    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;

    public PartidaController(PartidaRepository partidaRepository, UsuarioRepository usuarioRepository) {
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Lista todas las partidas almacenadas. */
    @GetMapping
    public List<PartidaEntity> listarPartidas() {
        return partidaRepository.findAll();
    }

    /**
     * Lista partidas por estado (EN_ESPERA, EN_CURSO, FINALIZADA, CAIDA_SERVIDOR).
     */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> listarPorEstado(@PathVariable String estado) {
        try {
            EstadoPartida estadoEnum = EstadoPartida.valueOf(estado.toUpperCase());
            return ResponseEntity.ok(partidaRepository.findByEstado(estadoEnum));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Estado inválido. Valores posibles: EN_ESPERA, EN_CURSO, FINALIZADA, CAIDA_SERVIDOR"));
        }
    }

    /** Obtiene una partida por su id. */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPartida(@PathVariable Long id) {
        return partidaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Elimina una partida por su id (operación administrativa). */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarPartida(@PathVariable Long id) {
        if (!partidaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        partidaRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Partida " + id + " eliminada correctamente"));
    }

    /** 
     * Crea una nueva partida en MySQL.
     * Llamado por el Middleware cuando un host crea una sala.
     */
    @PostMapping("/crear")
    public ResponseEntity<Long> crearPartida(@RequestParam String host) {
        UsuarioEntity hostEntity = usuarioRepository.findByUsername(host)
                .orElseThrow(() -> new RuntimeException("Host no encontrado"));
                
        PartidaEntity partida = new PartidaEntity(hostEntity, EstadoPartida.EN_ESPERA);
        partida = partidaRepository.save(partida);
        return ResponseEntity.ok(partida.getId());
    }

    /**
     * Actualiza el estado de la partida (EN_CURSO, FINALIZADA, etc).
     * Opcionalmente acepta el username del ganador para persistirlo en SQL.
     *
     * @param ganador (opcional) username del jugador ganador
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable Long id,
            @RequestParam String estado,
            @RequestParam(required = false) String ganador) {

        PartidaEntity partida = partidaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));

        partida.setEstado(EstadoPartida.valueOf(estado.toUpperCase()));

        if (estado.equalsIgnoreCase("FINALIZADA") || estado.equalsIgnoreCase("CAIDA_SERVIDOR")) {
            partida.setFechaFin(java.time.LocalDateTime.now());
        }

        // Registrar el ganador si se ha proporcionado su username
        if (ganador != null && !ganador.isBlank()) {
            usuarioRepository.findByUsername(ganador).ifPresent(partida::setGanador);
        }

        partidaRepository.save(partida);
        return ResponseEntity.ok().build();
    }


}
