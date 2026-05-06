package com.cifpaviles.proyectofinal.CLMM.middleware.sockets;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LobbyManager {
    private final Map<String, LobbyRoom> rooms = new ConcurrentHashMap<>();

    public void addRoom(LobbyRoom room) {
        rooms.put(room.getCodigoSala(), room);
    }

    public Optional<LobbyRoom> getRoom(String codigoSala) {
        return Optional.ofNullable(rooms.get(codigoSala));
    }

    public void removeRoom(String codigoSala) {
        rooms.remove(codigoSala);
    }

    public List<LobbyRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
}
