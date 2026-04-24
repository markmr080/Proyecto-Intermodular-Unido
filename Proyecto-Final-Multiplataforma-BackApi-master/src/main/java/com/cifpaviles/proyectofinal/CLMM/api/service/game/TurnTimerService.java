package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestiona el paso del tiempo en tiempo real.
 */
public class TurnTimerService {

    private final GameEngine gameEngine;
    private final ScheduledExecutorService scheduler;

    public TurnTimerService(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
        // Creamos un hilo que se ejecuta en segundo plano
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Inicia el segundero del juego.
     */
    public void iniciarCronometro() {
        scheduler.scheduleAtFixedRate(() -> {
            actualizarSegundo();
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void actualizarSegundo() {
        var state = gameEngine.getState();
        
        if (!state.isJuegoActivo()) return;

        int tiempoActual = state.getTiempoRestante();

        if (tiempoActual > 0) {
            // Restamos un segundo
            state.setTiempoRestante(tiempoActual - 1);
        } else {
            // Â¡TIEMPO AGOTADO!
            manejarTiempoAgotado();
        }
        
        // AquÃ­ es donde enviarÃ­as el estado por WebSocket a Angular
        enviarEstadoAFrontEnd(state);
    }

    private void manejarTiempoAgotado() {
        var state = gameEngine.getState();
        
        if (state.isFaseReaccion()) {
            // Si se acabaron los 20s de reacciÃ³n, el turno vuelve al original
            // o simplemente pasamos al siguiente turno normal.
            state.setMensajeEstado("Tiempo de reacciÃ³n agotado.");
            state.cambiarTurno();
        } else {
            // Si se acabaron los 60s de ronda normal, salta el turno.
            state.setMensajeEstado("Tiempo de turno agotado para " + state.getJugadorActivo().getNombre());
            state.cambiarTurno();
        }
    }

    private void enviarEstadoAFrontEnd(com.cifpaviles.proyectofinal.CLMM.api.model.game.GameState state) {
        // Este mÃ©todo se conectarÃ¡ con el WebSocketController mÃ¡s adelante.
        // Por ahora, imagina que aquÃ­ "disparamos" el JSON hacia Angular.
    }

    public void detener() {
        scheduler.shutdown();
    }
}
