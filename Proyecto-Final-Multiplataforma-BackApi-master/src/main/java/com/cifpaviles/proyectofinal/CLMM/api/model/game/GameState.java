package com.cifpaviles.proyectofinal.CLMM.api.model.game;

/**
 * Representa el estado global de una partida entre dos jugadores.
 * Es el objeto que se sincroniza con el frontend en tiempo real.
 */
public class GameState {
    private Player jugador1;
    private Player jugador2;
    
    // Control de Turnos
    private String turnoActualId; // ID del jugador que debe actuar
    private int tiempoRestante;   // Segundos para el cronÃ³metro (60s o 20s)
    
    // Estado de la partida
    private boolean juegoActivo;
    private String ganadorId;
    private String mensajeEstado; // Ej: "Â¡Jugador 1 ha usado un ataque especial!"
    
    // Flags de fase (Para cumplir tu regla de los 20s de reacción)
    private boolean faseReaccion; // true si estamos en los 20s de contraataque

    // Fase general: ESPERANDO, COLOCACION, COMBATE, FIN
    private String fase;

    // Referencia a MySQL
    private Long idPartidaMysql;
    private boolean statsGuardadas = false;

    public GameState(Player p1, Player p2) {
        this.jugador1 = p1;
        this.jugador2 = p2;
        this.turnoActualId = p1.getId(); // Empieza el jugador 1
        this.tiempoRestante = 60;        // Tiempo inicial de ronda
        this.juegoActivo = true;
        this.faseReaccion = false;
        this.fase = "COLOCACION";
        this.mensajeEstado = "Coloca tus barcos.";
    }

    /**
     * Alterna el turno entre los dos jugadores y reinicia el cronÃ³metro.
     */
    public void cambiarTurno() {
        if (turnoActualId.equals(jugador1.getId())) {
            turnoActualId = jugador2.getId();
        } else {
            turnoActualId = jugador1.getId();
        }
        
        // Reset de flags de turno para AMBOS jugadores.
        jugador1.setHabilidadUsadaEsteTurno(false);
        jugador1.setHaAtacadoEsteTurno(false);
        jugador1.setTurnoExtraWulfrik(false);
        jugador2.setHabilidadUsadaEsteTurno(false);
        jugador2.setHaAtacadoEsteTurno(false);
        jugador2.setTurnoExtraWulfrik(false);
        
        // Al cambiar turno, los cooldowns de las habilidades del nuevo jugador activo bajan
        getJugadorActivo().getPersonaje().getHabilidadesActivas().forEach(Skill::reducirCooldown);
        
        this.tiempoRestante = 60;
        this.faseReaccion = false;
    }

    /**
     * MÃ©todo de conveniencia para obtener el objeto Player que tiene el turno.
     */
    public Player getJugadorActivo() {
        return turnoActualId.equals(jugador1.getId()) ? jugador1 : jugador2;
    }

    /**
     * MÃ©todo de conveniencia para obtener el objeto Player que estÃ¡ esperando.
     */
    public Player getEnemigo() {
        return turnoActualId.equals(jugador1.getId()) ? jugador2 : jugador1;
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
    
    public void setTurnoActualId(String turnoActualId) {
        this.turnoActualId = turnoActualId;
    }

    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }

    public Long getIdPartidaMysql() { return idPartidaMysql; }
    public void setIdPartidaMysql(Long idPartidaMysql) { this.idPartidaMysql = idPartidaMysql; }

    public boolean isStatsGuardadas() { return statsGuardadas; }
    public void setStatsGuardadas(boolean statsGuardadas) { this.statsGuardadas = statsGuardadas; }
}
