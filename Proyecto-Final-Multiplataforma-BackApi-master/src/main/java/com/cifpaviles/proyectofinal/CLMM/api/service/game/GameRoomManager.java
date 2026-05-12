package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona el ciclo de vida de las salas de juego activas en memoria.
 * Cada sala tiene su propio GameEngine (motor de reglas) y TurnTimerService
 * (cronómetro compartido). Ambos se eliminan al finalizar la partida para
 * evitar fugas de memoria e hilos huérfanos.
 *
 * Referencia: GameSocketController lo usa para obtener/crear salas y
 * arrancar el cronómetro al iniciarse la fase COMBATE.
 */
@Service
public class GameRoomManager {

    // Motor de juego por sala (roomCode -> GameEngine)
    private final ConcurrentHashMap<String, GameEngine> activeRooms = new ConcurrentHashMap<>();
    // Cronómetro compartido por sala (roomCode -> TurnTimerService)
    private final ConcurrentHashMap<String, TurnTimerService> activeTimers = new ConcurrentHashMap<>();

    // Servidor de WebSockets necesario para crear TurnTimerService con capacidad de difusión
    private final SocketIOServer server;

    @Autowired
    public GameRoomManager(SocketIOServer server) {
        this.server = server;
    }

    /**
     * Obtiene el GameEngine de la sala o crea uno nuevo si no existe.
     * El timer NO se inicia aquí; se inicia explícitamente al comenzar COMBATE.
     */
    public GameEngine getOrCreateRoom(String roomCode) {
        return activeRooms.computeIfAbsent(roomCode, k -> new GameEngine(null));
    }

    /** Obtiene el GameEngine de una sala existente. Devuelve null si no existe. */
    public GameEngine getRoom(String roomCode) {
        return activeRooms.get(roomCode);
    }

    /**
     * Inicia el cronómetro compartido para la sala indicada.
     * Debe llamarse UNA sola vez al pasar a fase COMBATE.
     * El TurnTimerService difunde el gameState por WebSocket cada segundo.
     */
    public void startTimer(String roomCode) {
        GameEngine engine = activeRooms.get(roomCode);
        if (engine == null) return;

        // Evitamos crear un segundo timer si ya existe uno para esta sala
        activeTimers.computeIfAbsent(roomCode, k -> {
            TurnTimerService timer = new TurnTimerService(engine, server, roomCode);
            timer.iniciarCronometro();
            return timer;
        });
    }

    /**
     * Comprueba si una sala existe en memoria y su partida sigue activa.
     * Usado por el endpoint REST para que el frontend valide la reconexión.
     */
    public boolean isRoomActive(String roomCode) {
        GameEngine engine = activeRooms.get(roomCode);
        return engine != null
                && engine.getState() != null
                && engine.getState().isJuegoActivo();
    }

    /**
     * Elimina la sala de memoria y detiene su cronómetro.
     * Llamar al finalizar la partida para liberar recursos.
     */
    public void removeRoom(String roomCode) {
        activeRooms.remove(roomCode);
        TurnTimerService timer = activeTimers.remove(roomCode);
        if (timer != null) {
            timer.detener();
        }
    }
}
