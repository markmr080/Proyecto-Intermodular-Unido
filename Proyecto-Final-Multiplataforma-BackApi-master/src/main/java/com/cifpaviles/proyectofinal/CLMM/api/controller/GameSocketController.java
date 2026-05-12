package com.cifpaviles.proyectofinal.CLMM.api.controller;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.EstadoPartida;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.repository.PartidaRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEstadisticasService;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.cifpaviles.proyectofinal.CLMM.api.service.game.GameEngine;
import com.cifpaviles.proyectofinal.CLMM.api.service.game.GameRoomManager;
import com.cifpaviles.proyectofinal.CLMM.api.service.game.CharacterFactory;
import com.cifpaviles.proyectofinal.CLMM.api.model.game.GameState;
import com.cifpaviles.proyectofinal.CLMM.api.model.game.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class GameSocketController {

    private final SocketIOServer server;
    private final GameRoomManager roomManager;
    private final CharacterFactory characterFactory;
    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final IEstadisticasService estadisticasService;

    /**
     * Mapea sessionId → {roomCode, jugadorId} para saber de qué sala desconectar
     * cuando un cliente pierde la conexión abruptamente (cierre de pestaña).
     */
    private final ConcurrentHashMap<UUID, String[]> sessionToRoom = new ConcurrentHashMap<>();
    /**
     * Mapea jugadorId → UUID (sessionId) del socket activo del jugador.
     * Se usa para notificarle cuando su gracia de reconexión expira.
     */
    private final ConcurrentHashMap<String, UUID> jugadorSockets = new ConcurrentHashMap<>();
    /**
     * Mapea jugadorId → ScheduledFuture del timer de gracia de reconexión.
     * Si el jugador vuelve antes de 30s, se cancela el Future.
     */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> reconnectTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @Autowired
    public GameSocketController(SocketIOServer server, 
                                GameRoomManager roomManager, 
                                CharacterFactory characterFactory,
                                PartidaRepository partidaRepository,
                                UsuarioRepository usuarioRepository,
                                IEstadisticasService estadisticasService) {
        this.server = server;
        this.roomManager = roomManager;
        this.characterFactory = characterFactory;
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.estadisticasService = estadisticasService;
    }

    @PostConstruct
    public void start() {
        server.addListeners(this);
        server.start();
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        System.out.println("Cliente conectado: " + client.getSessionId());
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        // Limpiar el mapa de sockets activos del jugador
        String[] info = sessionToRoom.remove(client.getSessionId());
        // Limpiar también si era el socket activo del jugador en jugadorSockets
        jugadorSockets.values().remove(client.getSessionId());

        if (info == null) return; // No estaba en ninguna sala de juego activa

        String roomCode  = info[0];
        String jugadorId = info[1];
        String nombreJugador = info.length > 2 ? info[2] : jugadorId;

        GameEngine engine = roomManager.getRoom(roomCode);
        if (engine == null || engine.getState() == null || !engine.getState().isJuegoActivo()) return;

        System.out.println("[DESCONEXION] " + jugadorId + " se desconectó de sala " + roomCode + ". Grace period 30s.");

        // Notificar al rival para que muestre el modal de espera
        server.getRoomOperations(roomCode).sendEvent("jugador-desconectado", jugadorId + "|" + nombreJugador);

        // Programar la derrota automática si no regresa en 30 segundos
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            reconnectTimers.remove(jugadorId);
            GameEngine eng = roomManager.getRoom(roomCode);
            if (eng == null || eng.getState() == null || !eng.getState().isJuegoActivo()) return;

            GameState state = eng.getState();
            // Comprueba que el jugador que se fue sigue sin estar conectado
            // (si se reconectó, el flag habrá cambiado)
            if (state.isJugadorDesconectado(jugadorId)) {
                String ganadorId = state.getJugador1().getId().equals(jugadorId)
                        ? state.getJugador2().getId()
                        : state.getJugador1().getId();
                state.setJuegoActivo(false);
                state.setGanadorId(ganadorId);
                state.setMensajeEstado("¡" + state.getJugadorPorId(ganadorId).getNombre()
                        + " gana! El rival abandonó la partida.");
                difundirEstado(roomCode, state);

                // Notificar al jugador desconectado (si tiene un nuevo socket abierto,
                // por ejemplo desde el menú) que su reconexión ya no es posible.
                UUID socketDesconectado = jugadorSockets.get(jugadorId);
                if (socketDesconectado != null) {
                    var clienteDesconectado = server.getClient(socketDesconectado);
                    if (clienteDesconectado != null) {
                        clienteDesconectado.sendEvent("reconexion-expirada", roomCode);
                    }
                }

                limpiarSalaFinalizada(roomCode, eng);
                System.out.println("[DESCONEXION] Partida finalizada por abandono de " + jugadorId);
            }
        }, 30, TimeUnit.SECONDS);

        reconnectTimers.put(jugadorId, future);
        // Marcar al jugador como desconectado en el estado
        engine.getState().setJugadorDesconectado(jugadorId, true);
    }

    @OnEvent("join-room")
    public void onJoinRoom(SocketIOClient client, JoinMessage mensaje, AckRequest ackSender) {
        try {
            String roomCode = mensaje.getRoomCode();
            String jugadorId = mensaje.getJugadorId();
            System.out.println("JOIN-ROOM RECIBIDO. Jugador: " + jugadorId + " | Sala: " + roomCode);
            
            // Registrar la sesión en el mapa para detectar desconexiones
            client.joinRoom(roomCode);
            sessionToRoom.put(client.getSessionId(),
                new String[]{roomCode, jugadorId, mensaje.getJugadorNombre() != null ? mensaje.getJugadorNombre() : jugadorId});

            // Actualizar el mapa de sockets activos del jugador (para notificarle si su gracia expira)
            jugadorSockets.put(jugadorId, client.getSessionId());

            // Si hay un timer de gracia activo para este jugador, cancelarlo (se reconectó)
            ScheduledFuture<?> pendingTimer = reconnectTimers.remove(jugadorId);
            if (pendingTimer != null && !pendingTimer.isDone()) {
                pendingTimer.cancel(false);
                GameEngine eng = roomManager.getRoom(roomCode);
                if (eng != null && eng.getState() != null) {
                    eng.getState().setJugadorDesconectado(jugadorId, false);
                    server.getRoomOperations(roomCode).sendEvent("jugador-reconectado", jugadorId);
                    System.out.println("[RECONEXION] " + jugadorId + " se reconectó a tiempo.");
                }
            }
            
            GameEngine engine = roomManager.getOrCreateRoom(roomCode);
            
            if (engine.getState() == null || !engine.getState().isJuegoActivo()) {
                // Primera vez O partida anterior ya finalizada: crear estado nuevo limpio
                String tipoP1 = mensaje.getPersonajeId() != null ? mensaje.getPersonajeId() : "WULFRIK";
                Player p1 = new Player(jugadorId, mensaje.getJugadorNombre(), characterFactory.crearPersonaje(tipoP1));
                Player p2 = new Player("enemigo-dummy", "Esperando...", characterFactory.crearPersonaje("WULFRIK"));
                GameState newState = new GameState(p1, p2);
                engine.setState(newState);
                System.out.println("JOIN-ROOM: Estado creado/reseteado. J1=" + jugadorId + " personaje=" + tipoP1);
            } else {
                GameState state = engine.getState();
                boolean esJ1 = state.getJugador1().getId().equals(jugadorId);
                boolean esJ2 = state.getJugador2().getId().equals(jugadorId);
                
                if (esJ1 || esJ2) {
                    // Jugador ya registrado (reintento de join-room): solo readmitir en sala
                    System.out.println("JOIN-ROOM: Jugador " + jugadorId + " ya registrado. Readmitiendo en sala.");
                } else if (state.getJugador2().getId().equals("enemigo-dummy")) {
                    // Segundo jugador: reemplaza al dummy con su ID, nombre y personaje elegido
                    String tipoP2 = mensaje.getPersonajeId() != null ? mensaje.getPersonajeId() : "WULFRIK";
                    state.getJugador2().setId(jugadorId);
                    state.getJugador2().setNombre(mensaje.getJugadorNombre());
                    state.getJugador2().setPersonaje(characterFactory.crearPersonaje(tipoP2));
                    state.setMensajeEstado("Ambos jugadores conectados. Coloca tus barcos.");
                    System.out.println("JOIN-ROOM: J2=" + jugadorId + " personaje=" + tipoP2);
                }
            }
            
            difundirEstado(roomCode, engine.getState());
            System.out.println("JOIN-ROOM: Estado difundido a sala " + roomCode 
                + " | turnoActualId=" + engine.getState().getTurnoActualId());
        } catch (Exception e) {
            System.err.println("ERROR EN ONJOINROOM: ");
            e.printStackTrace();
        }
    }

    @OnEvent("atacar")
    public void onAtacar(SocketIOClient client, AtaqueMessage mensaje, AckRequest ackSender) {
        GameEngine engine = roomManager.getRoom(mensaje.getRoomCode());
        if (engine != null && engine.getState() != null) {
            String turnoAntes = engine.getState().getTurnoActualId();
            System.out.println("[ATACAR] jugadorId=" + mensaje.getJugadorId()
                + " | turnoAntes=" + turnoAntes
                + " | xy=(" + mensaje.getX() + "," + mensaje.getY() + ")");

            engine.procesarDisparo(mensaje.getJugadorId(), mensaje.getX(), mensaje.getY());

            String turnoDespues = engine.getState().getTurnoActualId();
            System.out.println("[ATACAR] resultado: turnoDespues=" + turnoDespues
                + (turnoAntes.equals(turnoDespues) ? " ⚠️ TURNO NO CAMBIÓ" : " ✅ turno cambiado"));

            difundirEstado(mensaje.getRoomCode(), engine.getState());

            // Limpiar la sala si el juego terminó (funciona en modo test y multijugador)
            limpiarSalaFinalizada(mensaje.getRoomCode(), engine);
        } else {
            System.out.println("[ATACAR] ERROR - engine o state nulo para sala: " + mensaje.getRoomCode());
        }
    }

    @OnEvent("usar-habilidad")
    public void onUsarHabilidad(SocketIOClient client, HabilidadMessage mensaje, AckRequest ackSender) {
        GameEngine engine = roomManager.getRoom(mensaje.getRoomCode());
        if (engine != null && engine.getState() != null) {
            // x,y son -1 para habilidades sin target; coordenada de celda para habilidades de área
            engine.usarHabilidad(mensaje.getJugadorId(), mensaje.getHabilidadId(), mensaje.getX(), mensaje.getY());
            difundirEstado(mensaje.getRoomCode(), engine.getState());
            // Una habilidad ofensiva puede terminar la partida (ej: hunde el último barco)
            limpiarSalaFinalizada(mensaje.getRoomCode(), engine);
        }
    }

    @OnEvent("colocar-barcos")
    public void onColocarBarcos(SocketIOClient client, ColocarBarcosMessage mensaje, AckRequest ackSender) {
        GameEngine engine = roomManager.getRoom(mensaje.getRoomCode());
        if (engine != null && engine.getState() != null) {
            GameState state = engine.getState();
            Player p = state.getJugador1().getId().equals(mensaje.getJugadorId()) ? state.getJugador1() : state.getJugador2();
            
            if (p != null) {
                com.cifpaviles.proyectofinal.CLMM.api.model.game.CellStatus[][] newTablero = new com.cifpaviles.proyectofinal.CLMM.api.model.game.CellStatus[10][10];
                int totalVidas = 0;
                for(int i=0; i<10; i++) {
                    for(int j=0; j<10; j++) {
                        com.cifpaviles.proyectofinal.CLMM.api.model.game.CellStatus st = com.cifpaviles.proyectofinal.CLMM.api.model.game.CellStatus.valueOf(mensaje.getTablero()[i][j]);
                        newTablero[i][j] = st;
                        if(st == com.cifpaviles.proyectofinal.CLMM.api.model.game.CellStatus.BARCO) totalVidas++;
                    }
                }
                p.setTablero(newTablero);
                p.setVidas(totalVidas);
                p.setListoParaCombate(true);
            }
            
            // Si ambos están listos, pasamos a COMBATE y persistimos en DB
            if (state.getJugador1().isListoParaCombate() && state.getJugador2().isListoParaCombate() && !state.getFase().equals("COMBATE")) {
                state.setFase("COMBATE");
                state.setMensajeEstado("¡Comienza la batalla! Turno de " + state.getJugadorActivo().getNombre());
                
                iniciarPartidaBD(state);
                
                // Arrancamos el cronómetro compartido para esta sala.
                // El TurnTimerService difundirá el gameState cada segundo a ambos clientes,
                // garantizando que los dos vean el mismo tiempo restante en pantalla.
                roomManager.startTimer(mensaje.getRoomCode());
            }
            
            difundirEstado(mensaje.getRoomCode(), state);
        }
    }

    /**
     * El jugador que se rinde pierde automáticamente.
     * El rival es declarado ganador, se difunde el estado final y se limpia la sala.
     */
    @OnEvent("rendirse")
    public void onRendirse(SocketIOClient client, RendirseMessage mensaje, AckRequest ackSender) {
        GameEngine engine = roomManager.getRoom(mensaje.getRoomCode());
        if (engine == null || engine.getState() == null) return;

        GameState state = engine.getState();
        if (!state.isJuegoActivo()) return; // Ya terminó, ignorar

        // El rival del que se rinde es el ganador
        String ganadorId = state.getJugador1().getId().equals(mensaje.getJugadorId())
                ? state.getJugador2().getId()
                : state.getJugador1().getId();

        state.setJuegoActivo(false);
        state.setGanadorId(ganadorId);
        state.setMensajeEstado("¡" + state.getJugadorPorId(ganadorId).getNombre() + " gana! El rival se ha rendido.");

        System.out.println("[RENDIRSE] " + mensaje.getJugadorId() + " se rinde. Ganador: " + ganadorId);

        difundirEstado(mensaje.getRoomCode(), state);
        limpiarSalaFinalizada(mensaje.getRoomCode(), engine);
    }

    private void iniciarPartidaBD(GameState state) {
        Optional<UsuarioEntity> hostOpt = usuarioRepository.findByUsername(state.getJugador1().getNombre());
        if (hostOpt.isPresent()) {
            PartidaEntity partida = new PartidaEntity(hostOpt.get(), EstadoPartida.EN_CURSO);
            partidaRepository.save(partida);
            state.setIdPartidaMysql(partida.getId());
        }
    }

    private void finalizarPartidaBD(GameState state) {
        if (state.getIdPartidaMysql() == null) return;

        Optional<PartidaEntity> partidaOpt = partidaRepository.findById(state.getIdPartidaMysql());
        if (partidaOpt.isPresent()) {
            PartidaEntity partida = partidaOpt.get();
            partida.setEstado(EstadoPartida.FINALIZADA);
            partida.setFechaFin(java.time.LocalDateTime.now());

            // Determinar ganador
            String ganadorNombre = state.getGanadorId().equals(state.getJugador1().getId()) 
                                    ? state.getJugador1().getNombre() 
                                    : state.getJugador2().getNombre();
            
            Optional<UsuarioEntity> ganadorOpt = usuarioRepository.findByUsername(ganadorNombre);
            ganadorOpt.ifPresent(partida::setGanador);

            partidaRepository.save(partida);

            // Guardar stats en MongoDB para Jugador 1
            guardarStatsJugador(state.getJugador1(), partida.getId());
            // Guardar stats en MongoDB para Jugador 2
            guardarStatsJugador(state.getJugador2(), partida.getId());
        }
    }

    private void guardarStatsJugador(Player player, Long idPartida) {
        Optional<UsuarioEntity> userOpt = usuarioRepository.findByUsername(player.getNombre());
        if (userOpt.isPresent()) {
            UsuarioEntity user = userOpt.get();
            estadisticasService.guardarStatsPartida(
                    idPartida, 
                    user.getId(), 
                    null, // idPersonaje (opcional, dejamos null de momento o podríamos buscar el PersonajeEntity)
                    player.getHitsAcertados(), 
                    player.getHitsFallados(), 
                    player.getBarcosHundidos(), 
                    user.getUsername()
            );
        }
    }

    private void difundirEstado(String roomCode, GameState state) {
        if (state != null) {
            server.getRoomOperations(roomCode).sendEvent("gameState", state);
        }
    }

    // --- DTOs ---
    public static class JoinMessage {
        private String jugadorId;
        private String jugadorNombre;
        private String roomCode;
        /** ID del personaje elegido (WULFRIK, AISLINN, LOKHIR, ARANESSA). Puede ser null (default WULFRIK). */
        private String personajeId;
        public String getJugadorId() { return jugadorId; }
        public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }
        public String getJugadorNombre() { return jugadorNombre; }
        public void setJugadorNombre(String jugadorNombre) { this.jugadorNombre = jugadorNombre; }
        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
        public String getPersonajeId() { return personajeId; }
        public void setPersonajeId(String personajeId) { this.personajeId = personajeId; }
    }

    // --- DTOs ---
    public static class AtaqueMessage {
        private String jugadorId;
        private int x;
        private int y;
        private String roomCode;
        public String getJugadorId() { return jugadorId; }
        public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    }

    public static class HabilidadMessage {
        private String jugadorId;
        private String habilidadId;
        private String roomCode;
        // Coordenadas de celda objetivo (requeridas por habilidades de área; -1 si no aplica)
        private int x = -1;
        private int y = -1;
        public String getJugadorId() { return jugadorId; }
        public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }
        public String getHabilidadId() { return habilidadId; }
        public void setHabilidadId(String habilidadId) { this.habilidadId = habilidadId; }
        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
    }

    public static class ColocarBarcosMessage {
        private String jugadorId;
        private String roomCode;
        private String[][] tablero; // matriz 10x10 de Strings ("AGUA", "BARCO")
        public String getJugadorId() { return jugadorId; }
        public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }
        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
        public String[][] getTablero() { return tablero; }
        public void setTablero(String[][] tablero) { this.tablero = tablero; }
    }

    /**
     * Comprueba si el juego terminó y, en ese caso:
     *  1. Guarda las estadísticas en BD de forma inmediata.
     *  2. Programa la eliminación de la sala con 10 s de retraso para que:
     *     - Ambos clientes reciban el gameState final antes de que la sala desaparezca.
     *     - Un posible evento 'rendirse' tardío no encuentre la sala ya eliminada.
     * Se llama tras cada disparo, habilidad ofensiva y evento rendirse.
     */
    private void limpiarSalaFinalizada(String roomCode, GameEngine engine) {
        GameState state = engine.getState();
        if (state == null || state.isJuegoActivo()) return;

        // Guardar stats en BD solo si hay partida MySQL registrada y no se guardaron aún
        if (state.getIdPartidaMysql() != null && !state.isStatsGuardadas()) {
            state.setStatsGuardadas(true);
            finalizarPartidaBD(state);
        }

        // Ejecutar la espera y eliminación en un hilo separado para no bloquear el socket
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(10_000);
                roomManager.removeRoom(roomCode);
                System.out.println("[SALA] Sala " + roomCode + " eliminada de memoria tras fin de partida.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /** DTO para el evento 'rendirse'. */
    public static class RendirseMessage {
        private String jugadorId;
        private String roomCode;
        public String getJugadorId() { return jugadorId; }
        public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }
        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    }
}

