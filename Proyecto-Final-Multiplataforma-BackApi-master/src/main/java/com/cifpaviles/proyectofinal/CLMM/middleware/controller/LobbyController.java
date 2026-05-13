package com.cifpaviles.proyectofinal.CLMM.middleware.controller;

import com.cifpaviles.proyectofinal.CLMM.middleware.sockets.LobbyManager;
import com.cifpaviles.proyectofinal.CLMM.middleware.sockets.LobbyRoom;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/lobby")
public class LobbyController {

    private final LobbyManager lobbyManager;
    private final com.cifpaviles.proyectofinal.CLMM.api.model.repository.PartidaRepository partidaRepository;
    private final com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository usuarioRepository;

    public LobbyController(LobbyManager lobbyManager, 
                           com.cifpaviles.proyectofinal.CLMM.api.model.repository.PartidaRepository partidaRepository,
                           com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository usuarioRepository) {
        this.lobbyManager = lobbyManager;
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<LobbyRoom> getLobbyRooms() {
        return lobbyManager.getAllRooms();
    }

    @PostMapping
    public ResponseEntity<LobbyRoom> createLobbyRoom(@RequestBody LobbyRoom room) {
        if (room.getCodigoSala() != null) {
            // Guardar en BD con estado EN_ESPERA
            if (room.getNombreJugador1() != null) {
                java.util.Optional<com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity> hostOpt = 
                    usuarioRepository.findByUsername(room.getNombreJugador1());
                if (hostOpt.isPresent()) {
                    com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity partida = 
                        new com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity(
                            hostOpt.get(), 
                            com.cifpaviles.proyectofinal.CLMM.api.model.entity.EstadoPartida.EN_ESPERA
                        );
                    partidaRepository.save(partida);
                    room.setIdPartidaMysql(partida.getId());
                }
            }
            lobbyManager.addRoom(room);
        }
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLobbyRoom(@PathVariable String id) {
        // En este contexto usamos el codigoSala como identificador principal
        lobbyManager.removeRoom(id);
        return ResponseEntity.noContent().build();
    }
}
