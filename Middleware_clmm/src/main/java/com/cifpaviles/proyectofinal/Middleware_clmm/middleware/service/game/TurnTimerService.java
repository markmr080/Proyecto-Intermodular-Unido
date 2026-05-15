package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.game;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.game.GameState;
import com.corundumstudio.socketio.SocketIOServer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TurnTimerService {
    private final GameEngine gameEngine;
    private final SocketIOServer server;
    private final String roomCode;
    private final ScheduledExecutorService scheduler;

    public TurnTimerService(GameEngine gameEngine, SocketIOServer server, String roomCode) {
        this.gameEngine = gameEngine;
        this.server = server;
        this.roomCode = roomCode;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void iniciarCronometro() {
        scheduler.scheduleAtFixedRate(this::actualizarSegundo, 0, 1, TimeUnit.SECONDS);
    }

    private void actualizarSegundo() {
        GameState state = gameEngine.getState();
        if (state == null || !state.isJuegoActivo()) {
            detener();
            return;
        }
        int tiempoActual = state.getTiempoRestante();
        if (tiempoActual > 0) {
            state.setTiempoRestante(tiempoActual - 1);
        } else {
            manejarTiempoAgotado(state);
        }
        difundirEstado(state);
    }

    private void manejarTiempoAgotado(GameState state) {
        if (state.isFaseReaccion()) {
            state.setMensajeEstado("Tiempo de reacción agotado.");
        } else {
            state.setMensajeEstado("Tiempo de turno agotado para " + state.getJugadorActivo().getNombre());
        }
        state.cambiarTurno();
    }

    private void difundirEstado(GameState state) {
        server.getRoomOperations(roomCode).sendEvent("gameState", state);
    }

    public void detener() {
        scheduler.shutdown();
    }
}
