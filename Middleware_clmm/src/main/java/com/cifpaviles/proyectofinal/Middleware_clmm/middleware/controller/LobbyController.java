package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.controller;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.sockets.LobbyManager;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.sockets.LobbyRoom;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lobby")
public class LobbyController {

    private final LobbyManager lobbyManager;
    private final BackendClient backendClient;

    public LobbyController(LobbyManager lobbyManager, BackendClient backendClient) {
        this.lobbyManager = lobbyManager;
        this.backendClient = backendClient;
    }

    @GetMapping
    public List<LobbyRoom> getLobbyRooms() {
        return lobbyManager.getAllRooms();
    }

    @PostMapping
    public ResponseEntity<LobbyRoom> createLobbyRoom(@RequestBody LobbyRoom room) {
        if (room.getCodigoSala() != null) {
            // Guardar en BD con estado EN_ESPERA usando el cliente HTTP
            if (room.getNombreJugador1() != null) {
                try {
                    Long idPartida = backendClient.crearPartida(room.getNombreJugador1());
                    room.setIdPartidaMysql(idPartida);
                } catch (Exception e) {
                    System.err.println("Error al crear partida en backend: " + e.getMessage());
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
