package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class GameRoomManager {
    
    private final ConcurrentHashMap<String, GameEngine> activeRooms = new ConcurrentHashMap<>();

    public GameEngine getOrCreateRoom(String roomId) {
        return activeRooms.computeIfAbsent(roomId, k -> new GameEngine(null));
    }

    public GameEngine getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    public void removeRoom(String roomId) {
        activeRooms.remove(roomId);
    }
}
