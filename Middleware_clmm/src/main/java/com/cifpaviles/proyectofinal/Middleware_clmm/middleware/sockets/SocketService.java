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
    private final com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient backendClient;

    private final Map<UUID, String> socketUsers = new ConcurrentHashMap<>();
    private final Map<String, UUID> userSockets = new ConcurrentHashMap<>();
    private final Map<UUID, String> sessionToGameRoom = new ConcurrentHashMap<>();

    @Autowired
    public SocketService(SocketIOServer server, LobbyManager lobbyManager, 
                         GameRoomManager roomManager, CharacterFactory characterFactory,
                         com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient backendClient) {
        this.server = server;
        this.lobbyManager = lobbyManager;
        this.roomManager = roomManager;
        this.characterFactory = characterFactory;
        this.backendClient = backendClient;
    }

    @PostConstruct
    public void setupListeners() {
        server.addConnectListener(cliente -> {
            System.out.println("Socket: Cliente conectado: " + cliente.getSessionId());
        });

        server.addDisconnectListener(cliente -> {
            String userId = socketUsers.get(cliente.getSessionId());
            String roomCode = sessionToGameRoom.remove(cliente.getSessionId());

            if (userId != null) {
                socketUsers.remove(cliente.getSessionId());
                userSockets.remove(userId);
                System.out.println("Socket: Usuario desconectado: " + userId);
                
                // Si estaba en una partida activa, manejar desconexión con periodo de gracia
                if (roomCode != null) {
                    GameEngine engine = roomManager.getRoom(roomCode);
                    if (engine != null && engine.getState() != null && engine.getState().isJuegoActivo()) {
                        System.out.println("Juego: Jugador " + userId + " se desconectó en la sala " + roomCode);

                        // Avisar SOLO al rival (no a la sala entera ni al propio desconectado)
                        String opponentId = engine.getState().getJugador1().getId().equals(userId)
                            ? engine.getState().getJugador2().getId()
                            : engine.getState().getJugador1().getId();
                        UUID opponentSocket = userSockets.get(opponentId);
                        if (opponentSocket != null && server.getClient(opponentSocket) != null) {
                            server.getClient(opponentSocket).sendEvent("jugador-desconectado", userId + "|" + userId);
                        }

                        // Hilo de 60s de gracia para reconectar
                        final String opponentIdFinal = opponentId;
                        new Thread(() -> {
                            try {
                                Thread.sleep(60000);
                                if (engine.getState() != null && engine.getState().isJuegoActivo()) {
                                    UUID newSocketId = userSockets.get(userId);
                                    String currentRoom = newSocketId != null ? sessionToGameRoom.get(newSocketId) : null;

                                    // Si no ha vuelto a entrar a la sala en 60s, se da por perdida
                                    if (!roomCode.equals(currentRoom)) {
                                        System.out.println("Juego: Jugador " + userId + " no reentró a la sala " + roomCode + " en 60s. Finalizando.");
                                        engine.finalizarJuego(opponentIdFinal, "¡FIN DE PARTIDA! El rival no se reconectó a tiempo.");
                                        // Eliminar sala del manager ANTES de notificar, para que isSalaActiva devuelva false
                                        roomManager.removeRoom(roomCode);
                                        // Notificar al rival (el que se quedó) el gameState final
                                        UUID oppSocket = userSockets.get(opponentIdFinal);
                                        if (oppSocket != null && server.getClient(oppSocket) != null) {
                                            server.getClient(oppSocket).sendEvent("gameState", engine.getState());
                                        }
                                    }
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    }
                }

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
                if (!"ESPERANDO".equals(sala.getEstado())) {
                    cliente.sendEvent("solicitud-rechazada", "La sala ya está llena o la partida ha comenzado.");
                    return;
                }
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

        server.addEventListener("rechazar-solicitud", Map.class, (cliente, data, ackRequest) -> {
            String requesterId = (String) data.get("requesterId");
            String mensaje = (String) data.get("mensaje");
            UUID rSocketId = userSockets.get(requesterId);
            if (rSocketId != null) {
                server.getClient(rSocketId).sendEvent("solicitud-rechazada", mensaje != null ? mensaje : "Tu solicitud ha sido rechazada.");
            }
        });

        server.addEventListener("iniciar-partida", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            server.getRoomOperations(codigoSala).sendEvent("partida-iniciada", codigoSala);
        });

        server.addEventListener("seleccionar-personaje", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            String userId = (String) data.get("userId");
            String personajeId = (String) data.get("personajeId");

            lobbyManager.getRoom(codigoSala).ifPresent(sala -> {
                if (userId.equals(sala.getJugador1())) {
                    sala.setPersonajeId1(personajeId);
                } else if (userId.equals(sala.getJugador2())) {
                    sala.setPersonajeId2(personajeId);
                }
            });

            server.getRoomOperations(codigoSala).sendEvent("personaje-seleccionado", data);
        });

        server.addEventListener("comenzar-juego", String.class, (cliente, codigoSala, ackRequest) -> {
            server.getRoomOperations(codigoSala).sendEvent("juego-comenzado", codigoSala);
        });

        server.addEventListener("abandonar-sala", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            String userId = (String) data.get("userId");
            lobbyManager.getRoom(codigoSala).ifPresent(sala -> {
                if (userId.equals(sala.getJugador1()) || !"ESPERANDO".equals(sala.getEstado())) {
                    server.getRoomOperations(codigoSala).sendEvent("sala-cerrada", "Un jugador abandonó. La sala ha sido cerrada.");
                    lobbyManager.removeRoom(codigoSala);
                    if (sala.getIdPartidaMysql() != null) {
                        try {
                            backendClient.eliminarPartida(sala.getIdPartidaMysql());
                        } catch (Exception e) {
                            System.err.println("Error eliminando partida fantasma en backend: " + e.getMessage());
                        }
                    }
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
            boolean esReconexion = false;
            if (engine.getState() == null) {
                // Primer jugador: crear estado inicial buscando si el rival ya eligió en el lobby
                LobbyRoom sala = lobbyManager.getRoom(roomCode).orElse(null);
                String p2Id = "WULFRIK";
                String p2Nombre = "Esperando...";
                if (sala != null) {
                    engine.setIdPartida(sala.getIdPartidaMysql());
                    if (jugadorId.equals(sala.getJugador1())) {
                        p2Id = (sala.getPersonajeId2() != null) ? sala.getPersonajeId2() : "WULFRIK";
                        p2Nombre = (sala.getNombreJugador2() != null) ? sala.getNombreJugador2() : "Esperando...";
                    } else {
                        // El que entra primero es el jugador 2? Raro pero posible
                        p2Id = (sala.getPersonajeId1() != null) ? sala.getPersonajeId1() : "WULFRIK";
                        p2Nombre = (sala.getNombreJugador1() != null) ? sala.getNombreJugador1() : "Esperando...";
                    }
                }
                engine.setState(new GameState(new Player(jugadorId, jugadorNombre, characterFactory.crearPersonaje(personajeId)), 
                                              new Player("dummy", p2Nombre, characterFactory.crearPersonaje(p2Id))));
            } else {
                GameState s = engine.getState();
                if (!s.getJugador1().getId().equals(jugadorId) && s.getJugador2().getId().equals("dummy")) {
                    // Segundo jugador uniéndose por primera vez
                    s.getJugador2().setId(jugadorId);
                    s.getJugador2().setNombre(jugadorNombre);
                    s.getJugador2().setPersonaje(characterFactory.crearPersonaje(personajeId));
                    s.setFase("COLOCACION");
                } else if (s.getJugador1().getId().equals(jugadorId) || s.getJugador2().getId().equals(jugadorId)) {
                    // Es una reconexión: el jugador ya estaba en la partida
                    esReconexion = true;
                    System.out.println("Juego: Reconexión de " + jugadorId + " a sala " + roomCode);
                }
            }

            // Enviar gameState al que se une/reconecta
            cliente.sendEvent("gameState", engine.getState());

            // Si es reconexión, avisar SOLO al rival para que cierre su modal de espera
            if (esReconexion) {
                String opponentId = engine.getState().getJugador1().getId().equals(jugadorId)
                    ? engine.getState().getJugador2().getId()
                    : engine.getState().getJugador1().getId();
                UUID opponentSocket = userSockets.get(opponentId);
                if (opponentSocket != null && server.getClient(opponentSocket) != null) {
                    server.getClient(opponentSocket).sendEvent("jugador-reconectado", jugadorId);
                }
            } else {
                // Primera unión: avisar a toda la sala y enviar el estado actualizado
                server.getRoomOperations(roomCode).sendEvent("jugador-reconectado", jugadorId);
                server.getRoomOperations(roomCode).sendEvent("gameState", engine.getState());
            }
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
                
                // Finalizamos el juego a través del engine para que guarde stats en Mongo y cambie estado en MySQL
                eng.finalizarJuego(ganadorId, "¡FIN DE PARTIDA! " + jugadorId + " se ha rendido.");
                
                server.getRoomOperations(rc).sendEvent("gameState", eng.getState());
            }
        });
        
        server.start();
    }
}
