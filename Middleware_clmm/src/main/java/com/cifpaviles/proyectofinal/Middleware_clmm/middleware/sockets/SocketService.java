package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.sockets;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.game.*;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.game.*;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SocketService {

    private final SocketIOServer server;
    private final LobbyManager lobbyManager;
    private final GameRoomManager roomManager;
    private final CharacterFactory characterFactory;

    private final Map<UUID, String> socketUsers = new ConcurrentHashMap<>();
    private final Map<String, UUID> userSockets = new ConcurrentHashMap<>();
    private final Map<UUID, String> sessionToGameRoom = new ConcurrentHashMap<>();

    @Autowired
    public SocketService(SocketIOServer server, LobbyManager lobbyManager, 
                         GameRoomManager roomManager, CharacterFactory characterFactory) {
        this.server = server;
        this.lobbyManager = lobbyManager;
        this.roomManager = roomManager;
        this.characterFactory = characterFactory;
    }

    @PostConstruct
    public void setupListeners() {
        server.addConnectListener(cliente -> {
            System.out.println("Socket: Cliente conectado: " + cliente.getSessionId());
        });

        server.addDisconnectListener(cliente -> {
            String userId = socketUsers.get(cliente.getSessionId());
            sessionToGameRoom.remove(cliente.getSessionId());
            if (userId != null) {
                socketUsers.remove(cliente.getSessionId());
                userSockets.remove(userId);
                System.out.println("Lobby: Usuario desconectado: " + userId);
                
                lobbyManager.getAllRooms().stream()
                        .filter(p -> userId.equals(p.getJugador1()) && "ESPERANDO".equals(p.getEstado()))
                        .forEach(p -> {
                            lobbyManager.removeRoom(p.getCodigoSala());
                            server.getBroadcastOperations().sendEvent("sala-cerrada", "Dueño desconectado");
                        });
            }
        });

        server.addEventListener("registrar-usuario", String.class, (cliente, userId, ackRequest) -> {
            socketUsers.put(cliente.getSessionId(), userId);
            userSockets.put(userId, cliente.getSessionId());
        });

        server.addEventListener("join-lobby", String.class, (cliente, roomCode, ackRequest) -> {
            cliente.joinRoom(roomCode);
            System.out.println("Socket: Cliente unido al lobby: " + roomCode);
        });

        server.addEventListener("solicitar-unirse", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            lobbyManager.getRoom(codigoSala).ifPresent(sala -> {
                UUID ownerSocketId = userSockets.get(sala.getJugador1());
                if (ownerSocketId != null) server.getClient(ownerSocketId).sendEvent("nueva-solicitud", data);
            });
        });

        server.addEventListener("aceptar-solicitud", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            String requesterId = (String) data.get("requesterId");
            lobbyManager.getRoom(codigoSala).ifPresent(sala -> {
                sala.setJugador2(requesterId);
                sala.setNombreJugador2((String) data.get("requesterName"));
                sala.setEstado("PREPARANDO");
                UUID rSocketId = userSockets.get(requesterId);
                if (rSocketId != null) server.getClient(rSocketId).sendEvent("solicitud-aceptada", codigoSala);
                cliente.sendEvent("jugador-unido", data);
            });
        });

        server.addEventListener("iniciar-partida", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            server.getRoomOperations(codigoSala).sendEvent("partida-iniciada", codigoSala);
        });

        server.addEventListener("seleccionar-personaje", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            server.getRoomOperations(codigoSala).sendEvent("personaje-seleccionado", data);
        });

        server.addEventListener("comenzar-juego", String.class, (cliente, codigoSala, ackRequest) -> {
            server.getRoomOperations(codigoSala).sendEvent("juego-comenzado", codigoSala);
        });

        server.addEventListener("abandonar-sala", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            String userId = (String) data.get("userId");
            lobbyManager.getRoom(codigoSala).ifPresent(sala -> {
                if (userId.equals(sala.getJugador1())) {
                    server.getRoomOperations(codigoSala).sendEvent("sala-cerrada", "Dueño abandonó");
                    lobbyManager.removeRoom(codigoSala);
                } else {
                    sala.setJugador2(null);
                    sala.setEstado("ESPERANDO");
                    server.getRoomOperations(codigoSala).sendEvent("partida-cancelada", userId);
                }
            });
        });

        server.addEventListener("join-room", Map.class, (cliente, data, ackRequest) -> {
            String roomCode = (String) data.get("roomCode");
            String jugadorId = (String) data.get("jugadorId");
            String jugadorNombre = (String) data.get("jugadorNombre");
            String personajeId = (String) data.get("personajeId");
            cliente.joinRoom(roomCode);
            sessionToGameRoom.put(cliente.getSessionId(), roomCode);
            GameEngine engine = roomManager.getOrCreateRoom(roomCode);
            if (engine.getState() == null) {
                engine.setState(new GameState(new Player(jugadorId, jugadorNombre, characterFactory.crearPersonaje(personajeId)), 
                                              new Player("dummy", "Esperando...", characterFactory.crearPersonaje("WULFRIK"))));
            } else {
                GameState s = engine.getState();
                if (!s.getJugador1().getId().equals(jugadorId) && s.getJugador2().getId().equals("dummy")) {
                    s.getJugador2().setId(jugadorId);
                    s.getJugador2().setNombre(jugadorNombre);
                    s.getJugador2().setPersonaje(characterFactory.crearPersonaje(personajeId));
                    s.setFase("COLOCACION");
                }
            }
            server.getRoomOperations(roomCode).sendEvent("gameState", engine.getState());
        });

        server.addEventListener("atacar", Map.class, (cliente, data, ackRequest) -> {
            String rc = (String) data.get("roomCode");
            GameEngine eng = roomManager.getRoom(rc);
            if (eng != null) {
                eng.procesarDisparo((String) data.get("jugadorId"), (int) data.get("x"), (int) data.get("y"));
                server.getRoomOperations(rc).sendEvent("gameState", eng.getState());
            }
        });

        server.addEventListener("colocar-barcos", Map.class, (cliente, data, ackRequest) -> {
            String rc = (String) data.get("roomCode");
            GameEngine eng = roomManager.getRoom(rc);
            if (eng != null) {
                GameState state = eng.getState();
                Player p = state.getJugadorPorId((String) data.get("jugadorId"));
                if (p != null) {
                    // Aplicar el tablero enviado por el frontend al modelo del servidor
                    java.util.List<?> filas = (java.util.List<?>) data.get("tablero");
                    if (filas != null) {
                        CellStatus[][] tablero = new CellStatus[10][10];
                        for (int i = 0; i < 10 && i < filas.size(); i++) {
                            java.util.List<?> columnas = (java.util.List<?>) filas.get(i);
                            for (int j = 0; j < 10 && j < columnas.size(); j++) {
                                String celda = String.valueOf(columnas.get(j));
                                tablero[i][j] = "BARCO".equals(celda) ? CellStatus.BARCO : CellStatus.AGUA;
                            }
                        }
                        p.setTablero(tablero);
                        // Contar vidas = total de celdas BARCO
                        int vidas = 0;
                        for (CellStatus[] fila : tablero)
                            for (CellStatus c : fila)
                                if (c == CellStatus.BARCO) vidas++;
                        p.setVidas(vidas);
                    }
                    p.setListoParaCombate(true);
                    if (state.getJugador1().isListoParaCombate() && state.getJugador2().isListoParaCombate()) {
                        state.setFase("COMBATE");
                        roomManager.startTimer(rc);
                    }
                }
                server.getRoomOperations(rc).sendEvent("gameState", state);
            }
        });

        server.addEventListener("usar-habilidad", Map.class, (cliente, data, ackRequest) -> {
            String rc = (String) data.get("roomCode");
            GameEngine eng = roomManager.getRoom(rc);
            if (eng != null) {
                eng.usarHabilidad((String) data.get("jugadorId"), (String) data.get("habilidadId"), (int) data.get("x"), (int) data.get("y"));
                server.getRoomOperations(rc).sendEvent("gameState", eng.getState());
            }
        });

        server.addEventListener("rendirse", Map.class, (cliente, data, ackRequest) -> {
            String rc = (String) data.get("roomCode");
            String jugadorId = (String) data.get("jugadorId");
            GameEngine eng = roomManager.getRoom(rc);
            if (eng != null && eng.getState() != null) {
                GameState state = eng.getState();
                // El ganador es el rival del que se rindió
                String ganadorId = state.getJugador1().getId().equals(jugadorId)
                    ? state.getJugador2().getId()
                    : state.getJugador1().getId();
                state.setJuegoActivo(false);
                state.setGanadorId(ganadorId);
                state.setMensajeEstado("¡FIN DE PARTIDA! " + jugadorId + " se ha rendido.");
                server.getRoomOperations(rc).sendEvent("gameState", state);
            }
        });
        
        server.start();
    }
}
