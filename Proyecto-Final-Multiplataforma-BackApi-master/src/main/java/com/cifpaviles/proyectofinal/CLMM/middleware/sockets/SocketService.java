package com.cifpaviles.proyectofinal.CLMM.middleware.sockets;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOServer;

import jakarta.annotation.PostConstruct;

@Component
public class SocketService {

    private final SocketIOServer server;
    private final LobbyManager lobbyManager;

    // Mapa para asociar userId con socketId (y viceversa para limpieza)
    private final Map<String, UUID> userSockets = new ConcurrentHashMap<>();
    private final Map<UUID, String> socketUsers = new ConcurrentHashMap<>();

    @Autowired
    public SocketService(SocketIOServer server, LobbyManager lobbyManager) {
        this.server = server;
        this.lobbyManager = lobbyManager;
    }

    @PostConstruct
    public void registrarListeners() {
        server.addConnectListener(cliente -> {
            System.out.println("Lobby: Cliente conectado: " + cliente.getSessionId());
        });

        server.addDisconnectListener(cliente -> {
            String userId = socketUsers.get(cliente.getSessionId());
            if (userId != null) {
                userSockets.remove(userId);
                socketUsers.remove(cliente.getSessionId());
                System.out.println("Lobby: Usuario desconectado: " + userId);

                // Limpieza automática: si el usuario era dueño de alguna sala abierta
                lobbyManager.getAllRooms().stream()
                        .filter(p -> userId.equals(p.getJugador1()) && "ESPERANDO".equals(p.getEstado()))
                        .forEach(p -> {
                            System.out.println(
                                    "Lobby: Limpiando sala fantasma del usuario " + userId + ": " + p.getCodigoSala());
                            String j2 = p.getJugador2();
                            if (j2 != null) {
                                UUID j2Socket = userSockets.get(j2);
                                if (j2Socket != null) {
                                    server.getClient(j2Socket).sendEvent("sala-cerrada",
                                            "El administrador se ha desconectado.");
                                }
                            }
                            lobbyManager.removeRoom(p.getCodigoSala());
                        });
            }
        });

        // Registro de usuario
        server.addEventListener("registrar-usuario", String.class, (cliente, userId, ackRequest) -> {
            userSockets.put(userId, cliente.getSessionId());
            socketUsers.put(cliente.getSessionId(), userId);
            System.out.println("Lobby: Usuario registrado: " + userId + " -> " + cliente.getSessionId());
        });

        // Jugador solicita unirse a una sala
        server.addEventListener("solicitar-unirse", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            Optional<LobbyRoom> partidaOpt = lobbyManager.getRoom(codigoSala);
            if (partidaOpt.isPresent()) {
                String ownerId = partidaOpt.get().getJugador1();
                UUID ownerSocketId = userSockets.get(ownerId);
                if (ownerSocketId != null) {
                    server.getClient(ownerSocketId).sendEvent("nueva-solicitud", data);
                }
            }
        });

        // Admin acepta la solicitud
        server.addEventListener("aceptar-solicitud", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            String requesterId = (String) data.get("requesterId");
            String requesterName = (String) data.get("requesterName");
            String requesterAvatar = (String) data.get("requesterAvatar");

            Optional<LobbyRoom> partidaOpt = lobbyManager.getRoom(codigoSala);
            if (partidaOpt.isPresent()) {
                LobbyRoom partida = partidaOpt.get();
                partida.setJugador2(requesterId);
                partida.setNombreJugador2(requesterName);
                partida.setAvatarJugador2(requesterAvatar);
                partida.setEstado("EN_CURSO");

                UUID requesterSocketId = userSockets.get(requesterId);
                if (requesterSocketId != null) {
                    server.getClient(requesterSocketId).sendEvent("solicitud-aceptada", codigoSala);
                }
                cliente.sendEvent("jugador-unido", data);
            }
        });

        // Admin rechaza la solicitud
        server.addEventListener("rechazar-solicitud", String.class, (cliente, requesterId, ackRequest) -> {
            UUID requesterSocketId = userSockets.get(requesterId);
            if (requesterSocketId != null) {
                server.getClient(requesterSocketId).sendEvent("solicitud-rechazada",
                        "El administrador ha rechazado tu solicitud.");
            }
        });

        // Cerrar sala manualmente
        server.addEventListener("cerrar-sala", String.class, (cliente, codigoSala, ackRequest) -> {
            Optional<LobbyRoom> partidaOpt = lobbyManager.getRoom(codigoSala);
            if (partidaOpt.isPresent()) {
                LobbyRoom partida = partidaOpt.get();
                String jugador2Id = partida.getJugador2();
                if (jugador2Id != null) {
                    UUID j2Socket = userSockets.get(jugador2Id);
                    if (j2Socket != null) {
                        server.getClient(j2Socket).sendEvent("sala-cerrada", "El administrador ha cerrado la sala.");
                    }
                }
                lobbyManager.removeRoom(codigoSala);
                System.out.println("Lobby: Sala cerrada manualmente: " + codigoSala);
            }
        });

        // Iniciar partida (sincronización)
        server.addEventListener("iniciar-partida", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            System.out.println("Lobby: Solicitud 'iniciar-partida' recibida para sala: " + codigoSala);
            Optional<LobbyRoom> partidaOpt = lobbyManager.getRoom(codigoSala);
            if (partidaOpt.isPresent()) {
                LobbyRoom partida = partidaOpt.get();
                String j1Id = partida.getJugador1();
                String j2Id = partida.getJugador2();
                System.out.println("Lobby: Sala encontrada. J1=" + j1Id + ", J2=" + j2Id);

                if (j2Id != null) {
                    UUID j2Socket = userSockets.get(j2Id);
                    if (j2Socket != null) {
                        System.out.println("Lobby: Notificando a J2 (" + j2Id + ") en socket " + j2Socket);
                        server.getClient(j2Socket).sendEvent("partida-iniciada", codigoSala);
                    } else {
                        System.out.println("Lobby: ERROR - No se encontró socket para J2: " + j2Id);
                    }
                }

                System.out.println("Lobby: Notificando a J1 (" + j1Id + ") en socket " + cliente.getSessionId());
                cliente.sendEvent("partida-iniciada", codigoSala);
            } else {
                System.out.println("Lobby: ERROR - No se encontró la sala: " + codigoSala);
            }
        });
        
        // Selección de personaje
        server.addEventListener("seleccionar-personaje", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            String userId = (String) data.get("userId");
            Integer personajeId = (Integer) data.get("personajeId");

            System.out.println(
                    "Lobby: Usuario " + userId + " seleccionó personaje " + personajeId + " en sala " + codigoSala);

            Optional<LobbyRoom> partidaOpt = lobbyManager.getRoom(codigoSala);
            if (partidaOpt.isPresent()) {
                LobbyRoom partida = partidaOpt.get();
                String j1Id = partida.getJugador1();
                String j2Id = partida.getJugador2();

                String targetId = userId.equals(j1Id) ? j2Id : j1Id;
                if (targetId != null) {
                    UUID targetSocket = userSockets.get(targetId);
                    if (targetSocket != null) {
                        server.getClient(targetSocket).sendEvent("personaje-seleccionado", data);
                    }
                }
            }
        });

        // Comenzar juego real (desde selección de personajes a partida-activa)
        server.addEventListener("comenzar-juego", String.class, (cliente, codigoSala, ackRequest) -> {
            System.out.println("Lobby: Iniciando partida activa en sala: " + codigoSala);
            Optional<LobbyRoom> partidaOpt = lobbyManager.getRoom(codigoSala);
            if (partidaOpt.isPresent()) {
                LobbyRoom partida = partidaOpt.get();
                String j1Id = partida.getJugador1();
                String j2Id = partida.getJugador2();

                UUID j1Socket = userSockets.get(j1Id);
                UUID j2Socket = (j2Id != null) ? userSockets.get(j2Id) : null;

                if (j1Socket != null)
                    server.getClient(j1Socket).sendEvent("juego-comenzado", codigoSala);
                if (j2Socket != null)
                    server.getClient(j2Socket).sendEvent("juego-comenzado", codigoSala);
            }
        });
    }
}
