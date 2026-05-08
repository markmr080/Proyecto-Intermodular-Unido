package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import com.cifpaviles.proyectofinal.CLMM.api.model.game.GameState;
import com.corundumstudio.socketio.SocketIOServer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestiona el paso del tiempo en tiempo real para UNA sala de juego concreta.
 * Cada segundo, actualiza el cronómetro en el GameState y lo DIFUNDE por WebSocket
 * a ambos jugadores, de modo que el timer sea idéntico en los dos clientes.
 *
 * Referencia al GameEngine de la sala y al SocketIOServer para poder emitir
 * el evento "gameState" a todos los conectados en el roomCode.
 */
public class TurnTimerService {

    private final GameEngine gameEngine;
    // Servidor de WebSockets necesario para difundir el estado por sala
    private final SocketIOServer server;
    // Código de sala al que se difunde el estado cada segundo
    private final String roomCode;
    private final ScheduledExecutorService scheduler;

    /**
     * @param gameEngine Motor de juego de esta sala
     * @param server     Servidor SocketIO para emitir el gameState
     * @param roomCode   Código de la sala (canal WebSocket)
     */
    public TurnTimerService(GameEngine gameEngine, SocketIOServer server, String roomCode) {
        this.gameEngine = gameEngine;
        this.server = server;
        this.roomCode = roomCode;
        // Un único hilo de fondo por sala
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Inicia el segundero del juego. Llama a este método en cuanto comienza
     * la fase COMBATE (al recibir los tableros de ambos jugadores).
     */
    public void iniciarCronometro() {
        scheduler.scheduleAtFixedRate(this::actualizarSegundo, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Se ejecuta cada segundo. Descuenta el tiempo y, si se agota, cambia el turno.
     * Al terminar, siempre difunde el estado actualizado a los clientes.
     */
    private void actualizarSegundo() {
        GameState state = gameEngine.getState();

        // Si la partida terminó, paramos el timer para liberar el hilo
        if (state == null || !state.isJuegoActivo()) {
            detener();
            return;
        }

        int tiempoActual = state.getTiempoRestante();

        if (tiempoActual > 0) {
            // Descontamos un segundo del cronómetro compartido
            state.setTiempoRestante(tiempoActual - 1);
        } else {
            // ¡TIEMPO AGOTADO! Manejamos el cambio de turno
            manejarTiempoAgotado(state);
        }

        // Difundimos el gameState actualizado a AMBOS clientes de la sala.
        // Esto garantiza que el timer se vea igual en los dos navegadores.
        difundirEstado(state);
    }

    /**
     * Gestiona el agotamiento del tiempo. Si estábamos en fase de reacción
     * (20s), el turno vuelve al atacante original. Si era el turno normal
     * (60s), simplemente lo saltamos.
     */
    private void manejarTiempoAgotado(GameState state) {
        if (state.isFaseReaccion()) {
            // Los 20s de contraataque se agotaron: fin de fase de reacción
            state.setMensajeEstado("Tiempo de reacción agotado.");
        } else {
            // Los 60s normales se agotaron: el jugador activo pierde el turno
            state.setMensajeEstado("Tiempo de turno agotado para " + state.getJugadorActivo().getNombre());
        }
        // En ambos casos cambiamos el turno, que reinicia el cronómetro a 60s
        state.cambiarTurno();
    }

    /**
     * Emite el evento "gameState" a todos los clientes conectados a la sala.
     * Es el mismo mecanismo que usa GameSocketController en onAtacar/onColocarBarcos.
     */
    private void difundirEstado(GameState state) {
        server.getRoomOperations(roomCode).sendEvent("gameState", state);
    }

    /**
     * Detiene el hilo del cronómetro. Llamar al finalizar la partida o
     * al eliminar la sala de memoria para evitar fugas de hilos.
     */
    public void detener() {
        scheduler.shutdown();
    }
}
