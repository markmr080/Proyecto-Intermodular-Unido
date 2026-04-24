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
     */
    public void procesarDisparo(String jugadorId, int x, int y) {
        // 1. Validar si es el turno del jugador
        if (!state.getTurnoActualId().equals(jugadorId)) return;

        Player atacante = state.getJugadorActivo();
        Player enemigo = state.getEnemigo();

        // 2. No permitir disparar dos veces en el mismo turno normal
        if (atacante.isHaAtacadoEsteTurno()) return;

        // 3. Ejecutar lÃ³gica de impacto
        CellStatus celdaDestino = enemigo.getTablero()[x][y];
        
        if (celdaDestino == CellStatus.BARCO) {
            enemigo.getTablero()[x][y] = CellStatus.TOCADO;
            enemigo.recibirDano();
            state.setMensajeEstado("Â¡Impacto de " + atacante.getNombre() + "!");
        } else if (celdaDestino == CellStatus.AGUA) {
            enemigo.getTablero()[x][y] = CellStatus.AGUA_GOLPEADA;
            state.setMensajeEstado(atacante.getNombre() + " ha fallado.");
        }

        atacante.setHaAtacadoEsteTurno(true);

        // 4. Aplicar REGLA DE 20 SEGUNDOS (segÃºn tu boceto)
        // Tras atacar, el oponente tiene 20s para reaccionar.
        activarFaseReaccion();
        
        // 5. Verificar si alguien ha ganado
        verificarVictoria();
    }

    /**
     * Activa la fase de contraataque rÃ¡pido.
     */
    private void activarFaseReaccion() {
        state.setFaseReaccion(true);
        state.setTiempoRestante(20); // Bajamos el cronÃ³metro a 20s
        // Cambiamos el ID del turno para que el otro pueda disparar
        state.setTurnoActualId(state.getEnemigo().getId());
    }

    /**
     * Procesa el uso de una habilidad activa.
     */
    public void usarHabilidad(String jugadorId, String habilidadId) {
        Player p = state.getJugadorActivo();

        if (!p.getId().equals(jugadorId)) return;
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
