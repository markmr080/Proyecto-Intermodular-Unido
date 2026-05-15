package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.controller;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.game.GameRoomManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/partidas")
public class PartidasController {

    private final GameRoomManager roomManager;

    public PartidasController(GameRoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @GetMapping("/sala-activa/{code}")
    public ResponseEntity<?> isSalaActiva(@PathVariable String code) {
        boolean activa = roomManager.isRoomActive(code);
        System.out.println("[API] Verificando sala " + code + ". Activa: " + activa);
        return ResponseEntity.ok(Map.of("activa", activa));
    }
}
