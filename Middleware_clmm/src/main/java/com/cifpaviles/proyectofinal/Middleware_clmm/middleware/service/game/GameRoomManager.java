package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.game;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameRoomManager {
    private final ConcurrentHashMap<String, GameEngine> activeRooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TurnTimerService> activeTimers = new ConcurrentHashMap<>();
    private final SocketIOServer server;
    private final com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient backendClient;

    @Autowired
    public GameRoomManager(SocketIOServer server,
                           com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient backendClient) {
        this.server = server;
        this.backendClient = backendClient;
    }

    public GameEngine getOrCreateRoom(String roomCode) {
        return activeRooms.computeIfAbsent(roomCode, k -> {
            GameEngine engine = new GameEngine(null);
            engine.setBackendClient(backendClient);
            return engine;
        });
    }

    public GameEngine getRoom(String roomCode) {
        return activeRooms.get(roomCode);
    }

    public void startTimer(String roomCode) {
        GameEngine engine = activeRooms.get(roomCode);
        if (engine == null) return;
        activeTimers.computeIfAbsent(roomCode, k -> {
            TurnTimerService timer = new TurnTimerService(engine, server, roomCode);
            timer.iniciarCronometro();
            return timer;
        });
    }

    public boolean isRoomActive(String roomCode) {
        GameEngine engine = activeRooms.get(roomCode);
        return engine != null && engine.getState() != null && engine.getState().isJuegoActivo();
    }

    public void removeRoom(String roomCode) {
        activeRooms.remove(roomCode);
        TurnTimerService timer = activeTimers.remove(roomCode);
        if (timer != null) {
            timer.detener();
        }
    }
}
