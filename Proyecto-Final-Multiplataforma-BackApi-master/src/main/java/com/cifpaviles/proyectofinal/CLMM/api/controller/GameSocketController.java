package com.cifpaviles.proyectofinal.CLMM.api.controller;

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

@Component
public class GameSocketController {

    private final SocketIOServer server;
    private final GameRoomManager roomManager;

    @Autowired
    public GameSocketController(SocketIOServer server, GameRoomManager roomManager) {
        this.server = server;
        this.roomManager = roomManager;
    }

    @PostConstruct
    public void start() {
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
        String roomCode = mensaje.getRoomCode();
        client.joinRoom(roomCode);
        
        GameEngine engine = roomManager.getOrCreateRoom(roomCode);
        
        // Si no hay estado, creamos uno con jugadores dummy (se actualizarán al unirse)
        if (engine.getState() == null) {
            // Inicialmente creamos dos jugadores vacíos, pero usamos la lógica de CharacterFactory
            // para asignarles un personaje base.
            Player p1 = new Player(mensaje.getJugadorId(), mensaje.getJugadorNombre(), CharacterFactory.crearPersonaje("ARTILLERO"));
            Player p2 = new Player("enemigo-dummy", "Esperando...", CharacterFactory.crearPersonaje("COMANDANTE"));
            GameState newState = new GameState(p1, p2);
            engine.setState(newState);
        } else {
            // Ya había un jugador, el segundo toma el control del p2
            GameState state = engine.getState();
            if (state.getJugador2().getId().equals("enemigo-dummy")) {
                state.getJugador2().setId(mensaje.getJugadorId());
                state.getJugador2().setNombre(mensaje.getJugadorNombre());
                state.setMensajeEstado("Ambos jugadores conectados. " + state.getMensajeEstado());
            }
        }
        
        difundirEstado(roomCode, engine.getState());
    }

    @OnEvent("atacar")
    public void onAtacar(SocketIOClient client, AtaqueMessage mensaje, AckRequest ackSender) {
        GameEngine engine = roomManager.getRoom(mensaje.getRoomCode());
        if (engine != null && engine.getState() != null) {
            engine.procesarDisparo(mensaje.getJugadorId(), mensaje.getX(), mensaje.getY());
            difundirEstado(mensaje.getRoomCode(), engine.getState());
        }
    }

    @OnEvent("usar-habilidad")
    public void onUsarHabilidad(SocketIOClient client, HabilidadMessage mensaje, AckRequest ackSender) {
        GameEngine engine = roomManager.getRoom(mensaje.getRoomCode());
        if (engine != null && engine.getState() != null) {
            engine.usarHabilidad(mensaje.getJugadorId(), mensaje.getHabilidadId());
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
                // message contains a 2D array of string, we need to map it to CellStatus
                com.cifpaviles.proyectofinal.CLMM.api.model.game.CellStatus[][] newTablero = new com.cifpaviles.proyectofinal.CLMM.api.model.game.CellStatus[10][10];
                for(int i=0; i<10; i++) {
                    for(int j=0; j<10; j++) {
                        newTablero[i][j] = com.cifpaviles.proyectofinal.CLMM.api.model.game.CellStatus.valueOf(mensaje.getTablero()[i][j]);
                    }
                }
                p.setTablero(newTablero);
                p.setListoParaCombate(true);
            }
            
            // Si ambos están listos, pasamos a COMBATE
            if (state.getJugador1().isListoParaCombate() && state.getJugador2().isListoParaCombate()) {
                state.setFase("COMBATE");
                state.setMensajeEstado("¡Comienza la batalla! Turno de " + state.getJugadorActivo().getNombre());
            }
            
            difundirEstado(mensaje.getRoomCode(), state);
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
        public String getJugadorId() { return jugadorId; }
        public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }
        public String getJugadorNombre() { return jugadorNombre; }
        public void setJugadorNombre(String jugadorNombre) { this.jugadorNombre = jugadorNombre; }
        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    }

    // --- DTOs ---
    public static class AtaqueMessage {
        private String jugadorId;
        private int x;
        private int y;
        private String roomCode;

        public String getJugadorId() {
            return jugadorId;
        }

        public void setJugadorId(String jugadorId) {
            this.jugadorId = jugadorId;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    }

    public static class HabilidadMessage {
        private String jugadorId;
        private String habilidadId;
        private String roomCode;

        public String getJugadorId() {
            return jugadorId;
        }

        public void setJugadorId(String jugadorId) {
            this.jugadorId = jugadorId;
        }

        public String getHabilidadId() {
            return habilidadId;
        }

        public void setHabilidadId(String habilidadId) {
            this.habilidadId = habilidadId;
        }

        public String getRoomCode() { return roomCode; }
        public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
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
