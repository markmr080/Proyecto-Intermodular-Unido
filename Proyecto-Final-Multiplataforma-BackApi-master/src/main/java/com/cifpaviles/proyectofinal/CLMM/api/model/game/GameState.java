package com.cifpaviles.proyectofinal.CLMM.api.model.game;

import java.util.HashSet;
import java.util.Set;

/**
 * Representa el estado global de una partida entre dos jugadores.
 * Es el objeto que se sincroniza con el frontend en tiempo real.
 */
public class GameState {
    private Player jugador1;
    private Player jugador2;

    // Control de Turnos
    private String turnoActualId; // ID del jugador que debe actuar
    private int tiempoRestante;   // Segundos para el cronómetro (60s o 20s)

    // Estado de la partida
    private boolean juegoActivo;
    private String ganadorId;
    private String mensajeEstado; // Ej: "¡Jugador 1 ha usado un ataque especial!"

    // Flags de fase
    private boolean faseReaccion; // true si estamos en los 20s de contraataque

    // Fase general: ESPERANDO, COLOCACION, COMBATE, FIN
    private String fase;

    // Referencia a MySQL
    private Long idPartidaMysql;
    private boolean statsGuardadas = false;

    /**
     * IDs de jugadores actualmente en periodo de gracia de reconexión.
     * No se envía al frontend (es estado interno del servidor).
     */
    private final Set<String> jugadoresDesconectados = new HashSet<>();

    public GameState(Player p1, Player p2) {
        this.jugador1 = p1;
        this.jugador2 = p2;
        this.turnoActualId = p1.getId();
        this.tiempoRestante = 60;
        this.juegoActivo = true;
        this.faseReaccion = false;
        this.fase = "COLOCACION";
        this.mensajeEstado = "Coloca tus barcos.";
    }

    /** Alterna el turno entre los dos jugadores y reinicia el cronómetro. */
    public void cambiarTurno() {
        if (turnoActualId.equals(jugador1.getId())) {
            turnoActualId = jugador2.getId();
        } else {
            turnoActualId = jugador1.getId();
        }

        jugador1.setHabilidadUsadaEsteTurno(false);
        jugador1.setHaAtacadoEsteTurno(false);
        jugador1.setTurnoExtraWulfrik(false);
        jugador2.setHabilidadUsadaEsteTurno(false);
        jugador2.setHaAtacadoEsteTurno(false);
        jugador2.setTurnoExtraWulfrik(false);

        getJugadorActivo().getPersonaje().getHabilidadesActivas().forEach(Skill::reducirCooldown);

        this.tiempoRestante = 60;
        this.faseReaccion = false;
    }

    /** Devuelve el jugador que tiene el turno activo. */
    public Player getJugadorActivo() {
        return turnoActualId.equals(jugador1.getId()) ? jugador1 : jugador2;
    }

    /** Devuelve el jugador que está esperando su turno. */
    public Player getEnemigo() {
        return turnoActualId.equals(jugador1.getId()) ? jugador2 : jugador1;
    }

    /** Devuelve el jugador por su ID. Devuelve null si no existe. */
    public Player getJugadorPorId(String id) {
        if (jugador1.getId().equals(id)) return jugador1;
        if (jugador2.getId().equals(id)) return jugador2;
        return null;
    }

    // --- Reconexión ---

    /** Marca o desmarca a un jugador como desconectado (en periodo de gracia). */
    public void setJugadorDesconectado(String jugadorId, boolean desconectado) {
        if (desconectado) {
            jugadoresDesconectados.add(jugadorId);
        } else {
            jugadoresDesconectados.remove(jugadorId);
        }
    }

    /** Comprueba si un jugador está actualmente en periodo de gracia (desconectado). */
    public boolean isJugadorDesconectado(String jugadorId) {
        return jugadoresDesconectados.contains(jugadorId);
    }

    // --- Getters y Setters ---

    public Player getJugador1() { return jugador1; }
    public Player getJugador2() { return jugador2; }
    public String getTurnoActualId() { return turnoActualId; }

    public int getTiempoRestante() { return tiempoRestante; }
    public void setTiempoRestante(int tiempoRestante) { this.tiempoRestante = tiempoRestante; }

    public boolean isJuegoActivo() { return juegoActivo; }
    public void setJuegoActivo(boolean juegoActivo) { this.juegoActivo = juegoActivo; }

    public String getGanadorId() { return ganadorId; }
    public void setGanadorId(String ganadorId) { this.ganadorId = ganadorId; }

    public String getMensajeEstado() { return mensajeEstado; }
    public void setMensajeEstado(String mensajeEstado) { this.mensajeEstado = mensajeEstado; }

    public boolean isFaseReaccion() { return faseReaccion; }
    public void setFaseReaccion(boolean faseReaccion) { this.faseReaccion = faseReaccion; }

    public void setTurnoActualId(String turnoActualId) { this.turnoActualId = turnoActualId; }

    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }

    public Long getIdPartidaMysql() { return idPartidaMysql; }
    public void setIdPartidaMysql(Long idPartidaMysql) { this.idPartidaMysql = idPartidaMysql; }

    public boolean isStatsGuardadas() { return statsGuardadas; }
    public void setStatsGuardadas(boolean statsGuardadas) { this.statsGuardadas = statsGuardadas; }
}
