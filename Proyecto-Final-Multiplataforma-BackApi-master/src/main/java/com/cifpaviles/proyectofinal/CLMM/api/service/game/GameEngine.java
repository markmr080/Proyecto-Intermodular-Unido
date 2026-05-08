package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import com.cifpaviles.proyectofinal.CLMM.api.model.game.*;

/**
 * Motor principal que aplica las reglas del juego.
 */
public class GameEngine {

    private GameState state;

    public GameEngine(GameState state) {
        this.state = state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    /**
     * Procesa un disparo a una coordenada.
     *
     * Flujo de turnos: un disparo válido siempre pasa el turno al otro jugador.
     * cambiarTurno() limpia los flags de ambos jugadores y reinicia el timer a 60s.
     * El TurnTimerService cambia el turno automáticamente si el tiempo se agota.
     */
    public void procesarDisparo(String jugadorId, int x, int y) {
        // 1. Validar que es el turno del jugador que dispara
        if (!state.getTurnoActualId().equals(jugadorId))
            return;

        Player atacante = state.getJugadorActivo();
        Player enemigo = state.getEnemigo();

        // 2. No permitir disparar dos veces en el mismo turno
        if (atacante.isHaAtacadoEsteTurno())
            return;

        // 3. Ejecutar lógica de impacto
        CellStatus celdaDestino = enemigo.getTablero()[x][y];

        if (celdaDestino == CellStatus.BARCO) {
            enemigo.getTablero()[x][y] = CellStatus.TOCADO;
            enemigo.recibirDano();
            atacante.incrementarHitsAcertados();

            boolean[] visited = new boolean[100];
            if (dfsSunkCheck(enemigo.getTablero(), x, y, visited)) {
                boolean[] visitedMark = new boolean[100];
                marcarHundido(enemigo.getTablero(), x, y, visitedMark);
                atacante.incrementarBarcosHundidos();
                state.setMensajeEstado("¡Barco HUNDIDO por " + atacante.getNombre() + "!");
            } else {
                state.setMensajeEstado("¡Impacto de " + atacante.getNombre() + "!");
            }
        } else if (celdaDestino == CellStatus.AGUA) {
            enemigo.getTablero()[x][y] = CellStatus.AGUA_GOLPEADA;
            atacante.incrementarHitsFallados();
            state.setMensajeEstado(atacante.getNombre() + " ha fallado.");
        }

        // 4. Pasar turno al otro jugador (resetea flags y timer a 60s)
        state.cambiarTurno();

        // 5. Verificar si alguien ha ganado
        verificarVictoria();
    }

    private boolean dfsSunkCheck(CellStatus[][] tablero, int x, int y, boolean[] visited) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10)
            return true;
        int idx = x * 10 + y;
        if (visited[idx])
            return true;
        visited[idx] = true;

        CellStatus status = tablero[x][y];
        if (status == CellStatus.AGUA || status == CellStatus.AGUA_GOLPEADA || status == CellStatus.HUNDIDO)
            return true;
        if (status == CellStatus.BARCO)
            return false;

        boolean up = dfsSunkCheck(tablero, x - 1, y, visited);
        boolean down = dfsSunkCheck(tablero, x + 1, y, visited);
        boolean left = dfsSunkCheck(tablero, x, y - 1, visited);
        boolean right = dfsSunkCheck(tablero, x, y + 1, visited);

        return up && down && left && right;
    }

    private void marcarHundido(CellStatus[][] tablero, int x, int y, boolean[] visited) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10)
            return;
        int idx = x * 10 + y;
        if (visited[idx])
            return;
        visited[idx] = true;

        if (tablero[x][y] == CellStatus.TOCADO) {
            tablero[x][y] = CellStatus.HUNDIDO;
            marcarHundido(tablero, x - 1, y, visited);
            marcarHundido(tablero, x + 1, y, visited);
            marcarHundido(tablero, x, y - 1, visited);
            marcarHundido(tablero, x, y + 1, visited);
        }
    }



    /**
     * Procesa el uso de una habilidad activa.
     */
    public void usarHabilidad(String jugadorId, String habilidadId) {
        Player p = state.getJugadorActivo();

        if (!p.getId().equals(jugadorId))
            return;
        if (p.isHabilidadUsadaEsteTurno()) {
            state.setMensajeEstado("Ya has usado una habilidad en este turno.");
            return;
        }

        // Buscar la habilidad en el personaje
        Skill habilidad = p.getPersonaje().getHabilidadesActivas().stream()
                .filter(s -> s.getId().equals(habilidadId))
                .findFirst().orElse(null);

        if (habilidad != null && habilidad.estaLista()) {
            // AquÃ­ llamarÃ­amos a un ejecutor de efectos especÃ­ficos
            ejecutarEfectoHabilidad(habilidad, p);

            habilidad.activarCooldown();
            p.setHabilidadUsadaEsteTurno(true);
            state.setMensajeEstado(p.getNombre() + " usÃ³ " + habilidad.getNombre());
        }
    }

    private void ejecutarEfectoHabilidad(Skill skill, Player owner) {
        // LÃ³gica programada para cada tipo de habilidad
        // Ejemplo: Si es OFENSIVA de Ã¡rea, marcar varias celdas.
    }

    private void verificarVictoria() {
        if (!state.getJugador1().estaVivo()) {
            finalizarJuego(state.getJugador2().getId());
        } else if (!state.getJugador2().estaVivo()) {
            finalizarJuego(state.getJugador1().getId());
        }
    }

    private void finalizarJuego(String ganadorId) {
        state.setJuegoActivo(false);
        state.setGanadorId(ganadorId);
        state.setMensajeEstado("Â¡FIN DE LA PARTIDA!");
    }

    public GameState getState() {
        return state;
    }
}
