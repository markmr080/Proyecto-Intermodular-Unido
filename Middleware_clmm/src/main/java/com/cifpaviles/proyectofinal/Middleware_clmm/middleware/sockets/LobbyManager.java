package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.sockets;

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
        // Asegurar que tenga fecha si no viene del cliente
        if (room.getFechaCreacion() == 0) {
            room.setFechaCreacion(System.currentTimeMillis());
        }
        rooms.put(room.getCodigoSala(), room);
    }

    public Optional<LobbyRoom> getRoom(String codigoSala) {
        return Optional.ofNullable(rooms.get(codigoSala));
    }

    public void removeRoom(String codigoSala) {
        rooms.remove(codigoSala);
    }

    public List<LobbyRoom> getAllRooms() {
        List<LobbyRoom> list = new ArrayList<>(rooms.values());
        // Ordenar por fecha de creación (descendente: más recientes primero)
        list.sort((a, b) -> Long.compare(b.getFechaCreacion(), a.getFechaCreacion()));
        return list;
    }
}
