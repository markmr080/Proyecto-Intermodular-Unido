package com.cifpaviles.proyectofinal.CLMM.api.controller;

import com.cifpaviles.proyectofinal.CLMM.middleware.sockets.LobbyManager;
import com.cifpaviles.proyectofinal.CLMM.middleware.sockets.LobbyRoom;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/lobby")
public class LobbyController {

    private final LobbyManager lobbyManager;

    public LobbyController(LobbyManager lobbyManager) {
        this.lobbyManager = lobbyManager;
    }

    @GetMapping
    public List<LobbyRoom> getLobbyRooms() {
        return lobbyManager.getAllRooms();
    }

    @PostMapping
    public ResponseEntity<LobbyRoom> createLobbyRoom(@RequestBody LobbyRoom room) {
        // Asignar ID simulado si el frontend lo requiere para no romper
        if (room.getCodigoSala() != null) {
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
