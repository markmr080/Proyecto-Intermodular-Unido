package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.controller;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.game.GameRoomManager;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Proxy de partidas hacia el Backend API.
 * - /sala-activa/{code}  → estado en memoria (GameRoomManager, sin BD)
 * - /                    → historial completo (Backend API / MySQL)
 * - /estado/{estado}     → historial filtrado por estado (Backend API / MySQL)
 * - /{id}                → detalle de una partida (Backend API / MySQL)
 */
@RestController
@RequestMapping("/api/partidas")
public class PartidasController {

    private final GameRoomManager roomManager;
    private final BackendClient backendClient;

    public PartidasController(GameRoomManager roomManager, BackendClient backendClient) {
        this.roomManager = roomManager;
        this.backendClient = backendClient;
    }

    /** Comprueba si una sala sigue activa en memoria (sin JWT). */
    @GetMapping("/sala-activa/{code}")
    public ResponseEntity<?> isSalaActiva(@PathVariable String code) {
        boolean activa = roomManager.isRoomActive(code);
        System.out.println("[Middleware] Verificando sala " + code + ". Activa: " + activa);
        return ResponseEntity.ok(Map.of("activa", activa));
    }

    /** Lista todas las partidas persistidas en MySQL (requiere JWT). */
    @GetMapping
    public ResponseEntity<?> listarPartidas() {
        try {
            List<?> partidas = backendClient.listarPartidas();
            return ResponseEntity.ok(partidas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error consultando partidas: " + e.getMessage()));
        }
    }

    /** Lista partidas filtradas por estado: EN_ESPERA, EN_CURSO, FINALIZADA, CAIDA_SERVIDOR. */
    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> listarPorEstado(@PathVariable String estado) {
        try {
            List<?> partidas = backendClient.listarPartidasPorEstado(estado);
            return ResponseEntity.ok(partidas);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Propagar el 400 del backend si el estado es inválido
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getResponseBodyAsString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error consultando partidas por estado: " + e.getMessage()));
        }
    }

    /** Obtiene el detalle de una partida por su ID (requiere JWT). */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPartida(@PathVariable Long id) {
        try {
            Object partida = backendClient.obtenerPartida(id);
            return ResponseEntity.ok(partida);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error consultando partida: " + e.getMessage()));
        }
    }
}
