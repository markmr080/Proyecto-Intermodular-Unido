package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.game;

import java.util.HashSet;
import java.util.Set;

public class GameState {
    private Player jugador1;
    private Player jugador2;
    private String turnoActualId;
    private int tiempoRestante;
    private boolean juegoActivo;
    private String ganadorId;
    private String mensajeEstado;
    private boolean faseReaccion;
    private String fase;
    private Long idPartidaMysql;
    private boolean statsGuardadas = false;
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

    public Player getJugadorActivo() {
        return turnoActualId.equals(jugador1.getId()) ? jugador1 : jugador2;
    }

    public Player getEnemigo() {
        return turnoActualId.equals(jugador1.getId()) ? jugador2 : jugador1;
    }

    public Player getJugadorPorId(String id) {
        if (jugador1.getId().equals(id)) return jugador1;
        if (jugador2.getId().equals(id)) return jugador2;
        return null;
    }

    public void setJugadorDesconectado(String jugadorId, boolean desconectado) {
        if (desconectado) {
            jugadoresDesconectados.add(jugadorId);
        } else {
            jugadoresDesconectados.remove(jugadorId);
        }
    }

    public boolean isJugadorDesconectado(String jugadorId) {
        return jugadoresDesconectados.contains(jugadorId);
    }

    public Player getJugador1() { return jugador1; }
    public void setJugador1(Player p) { this.jugador1 = p; }
    public Player getJugador2() { return jugador2; }
    public void setJugador2(Player p) { this.jugador2 = p; }
    public String getTurnoActualId() { return turnoActualId; }
    public void setTurnoActualId(String turnoActualId) { this.turnoActualId = turnoActualId; }
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
    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }
    public Long getIdPartidaMysql() { return idPartidaMysql; }
    public void setIdPartidaMysql(Long idPartidaMysql) { this.idPartidaMysql = idPartidaMysql; }
    public boolean isStatsGuardadas() { return statsGuardadas; }
    public void setStatsGuardadas(boolean statsGuardadas) { this.statsGuardadas = statsGuardadas; }
}
