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

@Component
public class GameSocketController {

    private final SocketIOServer server;
    private final GameRoomManager roomManager;
    private final CharacterFactory characterFactory;
    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final IEstadisticasService estadisticasService;

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
        System.out.println("Cliente desconectado: " + client.getSessionId());
        // En una implementación real, buscaríamos de qué sala era y le daríamos por perdido.
    }

    @OnEvent("join-room")
    public void onJoinRoom(SocketIOClient client, JoinMessage mensaje, AckRequest ackSender) {
        try {
            String roomCode = mensaje.getRoomCode();
            String jugadorId = mensaje.getJugadorId();
            System.out.println("JOIN-ROOM RECIBIDO. Jugador: " + jugadorId + " | Sala: " + roomCode);
            
            // Añadir al canal Socket.IO de la sala (idempotente si ya está)
            client.joinRoom(roomCode);
            
            GameEngine engine = roomManager.getOrCreateRoom(roomCode);
            
            if (engine.getState() == null) {
                // Primer jugador: crea el estado con su personaje elegido y J2 dummy temporal
                String tipoP1 = mensaje.getPersonajeId() != null ? mensaje.getPersonajeId() : "WULFRIK";
                Player p1 = new Player(jugadorId, mensaje.getJugadorNombre(), characterFactory.crearPersonaje(tipoP1));
                Player p2 = new Player("enemigo-dummy", "Esperando...", characterFactory.crearPersonaje("WULFRIK"));
                GameState newState = new GameState(p1, p2);
                engine.setState(newState);
                System.out.println("JOIN-ROOM: Estado creado. J1=" + jugadorId + " personaje=" + tipoP1);
            } else {
                GameState state = engine.getState();
                boolean esJ1 = state.getJugador1().getId().equals(jugadorId);
                boolean esJ2 = state.getJugador2().getId().equals(jugadorId);
                
                if (esJ1 || esJ2) {
                    // Jugador ya registrado (reintento de join-room): solo readmitir en sala
                    System.out.println("JOIN-ROOM: Jugador " + jugadorId + " ya registrado. Readmitiendo en sala.");
                } else if (state.getJugador2().getId().equals("enemigo-dummy")) {
                    // Segundo jugador: reemplaza al dummy
                    state.getJugador2().setId(jugadorId);
                    state.getJugador2().setNombre(mensaje.getJugadorNombre());
                    state.setMensajeEstado("Ambos jugadores conectados. Coloca tus barcos.");
                    System.out.println("JOIN-ROOM: J2=" + jugadorId + " se ha unido. TurnoActual=" + state.getTurnoActualId());
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
                + " | J1=" + engine.getState().getJugador1().getId()
                + " | J2=" + engine.getState().getJugador2().getId());

            engine.procesarDisparo(mensaje.getJugadorId(), mensaje.getX(), mensaje.getY());

            System.out.println("[ATACAR] turnoAhora=" + engine.getState().getTurnoActualId()
                + " | tiempoRestante=" + engine.getState().getTiempoRestante());
            
            // Verificar si el juego terminó tras el disparo
            if (!engine.getState().isJuegoActivo() && engine.getState().getIdPartidaMysql() != null && !engine.getState().isStatsGuardadas()) {
                engine.getState().setStatsGuardadas(true);
                finalizarPartidaBD(engine.getState());
                // Removemos la sala de memoria para evitar que se reutilice con datos corruptos si juegan otra vez
                roomManager.removeRoom(mensaje.getRoomCode());
            }

            difundirEstado(mensaje.getRoomCode(), engine.getState());
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
}
