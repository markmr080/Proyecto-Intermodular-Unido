package com.cifpaviles.proyectofinal.CLMM.middleware.sockets;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.EstadoPartida;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.repository.PartidaRepository;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

import jakarta.annotation.PostConstruct;

@Component
public class SocketService {

    private SocketIOServer server;

    @Autowired
    private PartidaRepository partidaRepository;

    // Mapa para asociar userId con socketId (y viceversa para limpieza)
    private final Map<String, UUID> userSockets = new ConcurrentHashMap<>();
    private final Map<UUID, String> socketUsers = new ConcurrentHashMap<>();

    @PostConstruct
    public void iniciarServidor() {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(8085);

        server = new SocketIOServer(config);

        server.addConnectListener(cliente -> {
            System.out.println("Cliente conectado: " + cliente.getSessionId());
        });

        server.addDisconnectListener(cliente -> {
            String userId = socketUsers.get(cliente.getSessionId());
            if (userId != null) {
                userSockets.remove(userId);
                socketUsers.remove(cliente.getSessionId());
                System.out.println("Usuario desconectado: " + userId);

                // Limpieza automática: si el usuario era dueño de alguna sala abierta, la borramos
                partidaRepository.findAll().stream()
                    .filter(p -> userId.equals(p.getJugador1()))
                    .forEach(p -> {
                        System.out.println("Limpiando sala fantasma del usuario " + userId + ": " + p.getCodigoSala());
                        String j2 = p.getJugador2();
                        if (j2 != null) {
                            UUID j2Socket = userSockets.get(j2);
                            if (j2Socket != null) {
                                server.getClient(j2Socket).sendEvent("sala-cerrada", "El administrador se ha desconectado.");
                            }
                        }
                        partidaRepository.delete(p);
                    });
            }
        });

        // Registro de usuario
        server.addEventListener("registrar-usuario", String.class, (cliente, userId, ackRequest) -> {
            userSockets.put(userId, cliente.getSessionId());
            socketUsers.put(cliente.getSessionId(), userId);
            System.out.println("Usuario registrado: " + userId + " -> " + cliente.getSessionId());
        });

        // Jugador solicita unirse a una sala
        server.addEventListener("solicitar-unirse", Map.class, (cliente, data, ackRequest) -> {
            String codigoSala = (String) data.get("codigoSala");
            Optional<PartidaEntity> partidaOpt = partidaRepository.findByCodigoSala(codigoSala);
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

            Optional<PartidaEntity> partidaOpt = partidaRepository.findByCodigoSala(codigoSala);
            if (partidaOpt.isPresent()) {
                PartidaEntity partida = partidaOpt.get();
                partida.setJugador2(requesterId);
                partida.setNombreJugador2(requesterName);
                partida.setAvatarJugador2(requesterAvatar);
                partida.setEstado(EstadoPartida.EN_CURSO);
                partidaRepository.save(partida);

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
                server.getClient(requesterSocketId).sendEvent("solicitud-rechazada", "El administrador ha rechazado tu solicitud.");
            }
        });

        // Cerrar sala manualmente
        server.addEventListener("cerrar-sala", String.class, (cliente, codigoSala, ackRequest) -> {
            Optional<PartidaEntity> partidaOpt = partidaRepository.findByCodigoSala(codigoSala);
            if (partidaOpt.isPresent()) {
                PartidaEntity partida = partidaOpt.get();
                String jugador2Id = partida.getJugador2();
                if (jugador2Id != null) {
                    UUID j2Socket = userSockets.get(jugador2Id);
                    if (j2Socket != null) {
                        server.getClient(j2Socket).sendEvent("sala-cerrada", "El administrador ha cerrado la sala.");
                    }
                }
                partidaRepository.delete(partida);
                System.out.println("Sala cerrada manualmente: " + codigoSala);
            }
        });

        server.start();
    }

    public void stopServidor() {
        if (server != null) {
            server.stop();
        }
    }
}
