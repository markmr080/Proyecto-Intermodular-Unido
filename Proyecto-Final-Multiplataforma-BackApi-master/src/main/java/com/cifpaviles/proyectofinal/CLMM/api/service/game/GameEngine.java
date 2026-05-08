package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import com.cifpaviles.proyectofinal.CLMM.api.model.game.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Motor principal que aplica las reglas del juego.
 * Contiene la lógica de disparo, habilidades activas y pasivas.
 *
 * Pasivas aplicadas en procesarDisparo():
 *   - PAS_WUL (Wulfrik): disparo extra al acertar un BARCO
 *   - PAS_AIS (Aislinn): 20% de esquivar un impacto entrante
 *   - PAS_LOK (Lokhir): al hundir, revela una celda BARCO adyacente del enemigo
 *   - PAS_ARA (Aranessa): las celdas con escudo absorben el primer impacto
 *
 * Habilidades activas gestionadas en ejecutarEfectoHabilidad():
 *   SKL_WUL_1, SKL_WUL_2, SKL_WUL_3
 *   SKL_AIS_1, SKL_AIS_2, SKL_AIS_3
 *   SKL_LOK_1, SKL_LOK_2, SKL_LOK_3
 *   SKL_ARA_1, SKL_ARA_2, SKL_ARA_3
 */
public class GameEngine {

    private GameState state;

    public GameEngine(GameState state) { this.state = state; }

    public void setState(GameState state) { this.state = state; }

    // ============================================================
    //  DISPARO NORMAL
    // ============================================================

    /**
     * Procesa un disparo a una coordenada del tablero enemigo.
     * Integra las pasivas de todos los personajes.
     */
    public void procesarDisparo(String jugadorId, int x, int y) {
        if (!state.getTurnoActualId().equals(jugadorId)) return;

        Player atacante = state.getJugadorActivo();
        Player enemigo  = state.getEnemigo();

        // Bloquear doble disparo, salvo que sea el turno extra de Wulfrik
        if (atacante.isHaAtacadoEsteTurno() && !atacante.isTurnoExtraWulfrik()) return;

        CellStatus celda = enemigo.getTablero()[x][y];

        // No repetir casillas ya atacadas ni reveladas por vision
        if (celda == CellStatus.AGUA_GOLPEADA || celda == CellStatus.TOCADO || celda == CellStatus.HUNDIDO) return;
        // Las celdas REVELADAS sí se pueden atacar (el jugador conoce la posicion)
        // pero se tratan como BARCO en el impacto real (el estado es simplemente informativo).

        // Consumir el flag de turno extra si procede
        boolean eraDisparoExtra = atacante.isTurnoExtraWulfrik();
        if (eraDisparoExtra) atacante.setTurnoExtraWulfrik(false);
        atacante.setHaAtacadoEsteTurno(true);

        // Aranessa SKL_ARA_3: escudo total activo — el disparo falla automáticamente
        if (enemigo.isEscudoTotalActivo()) {
            enemigo.setEscudoTotalActivo(false);
            atacante.incrementarHitsFallados();
            state.setMensajeEstado("¡El Escudo de Stromfels protegió a " + enemigo.getNombre() + "! Disparo bloqueado.");
            state.cambiarTurno();
            return;
        }

        if (celda == CellStatus.BARCO || celda == CellStatus.REVELADA) {
            // Wulfrik SKL_WUL_3 / Aislinn SKL_AIS_3 / Lokhir SKL_LOK_3: escudo de casilla
            if (enemigo.tieneEscudo(x, y)) {
                enemigo.quitarEscudo(x, y);
                enemigo.getTablero()[x][y] = CellStatus.AGUA_GOLPEADA;
                atacante.incrementarHitsFallados();
                state.setMensajeEstado("¡Escudo! El impacto de " + atacante.getNombre() + " fue absorbido.");
                state.cambiarTurno();
                return;
            }

            // Aislinn PAS_AIS: 20% de esquivar
            if (tieneHabilidadPasiva(enemigo, "PAS_AIS") && Math.random() < 0.20) {
                enemigo.getTablero()[x][y] = CellStatus.AGUA_GOLPEADA;
                atacante.incrementarHitsFallados();
                state.setMensajeEstado("¡Bruma del Mar! Aislinn esquivó el impacto.");
                state.cambiarTurno();
                return;
            }

            // Impacto real
            enemigo.getTablero()[x][y] = CellStatus.TOCADO;
            enemigo.recibirDano();
            atacante.incrementarHitsAcertados();

            boolean[] vis = new boolean[100];
            if (dfsSunkCheck(enemigo.getTablero(), x, y, vis)) {
                boolean[] visMark = new boolean[100];
                marcarHundido(enemigo.getTablero(), x, y, visMark);
                atacante.incrementarBarcosHundidos();
                state.setMensajeEstado("¡Barco HUNDIDO por " + atacante.getNombre() + "!");
                // Lokhir PAS_LOK: revela celda adyacente al hundido
                if (tieneHabilidadPasiva(atacante, "PAS_LOK")) {
                    revelarCeldaAdyacente(enemigo, x, y);
                }
            } else {
                state.setMensajeEstado("¡Impacto de " + atacante.getNombre() + "!");
            }

            // Wulfrik PAS_WUL: disparo extra en el mismo turno (solo una vez)
            if (tieneHabilidadPasiva(atacante, "PAS_WUL") && !eraDisparoExtra) {
                atacante.setTurnoExtraWulfrik(true);
                atacante.setHaAtacadoEsteTurno(false); // permitir siguiente disparo
                state.setMensajeEstado(state.getMensajeEstado() + " ¡Wulfrik dispara de nuevo!");
                verificarVictoria();
                return; // NO cambia el turno todavía
            }

        } else if (celda == CellStatus.AGUA) {
            enemigo.getTablero()[x][y] = CellStatus.AGUA_GOLPEADA;
            atacante.incrementarHitsFallados();
            state.setMensajeEstado(atacante.getNombre() + " ha fallado.");
        }

        state.cambiarTurno();
        verificarVictoria();
    }

    // ============================================================
    //  HABILIDADES ACTIVAS
    // ============================================================

    /**
     * Procesa el uso de una habilidad activa.
     * Las habilidades de área usan las coordenadas x,y; las demás ignoran -1,-1.
     */
    public void usarHabilidad(String jugadorId, String habilidadId, int x, int y) {
        Player p = state.getJugadorActivo();
        if (!p.getId().equals(jugadorId)) return;
        if (p.isHabilidadUsadaEsteTurno()) {
            state.setMensajeEstado("Ya has usado una habilidad este turno.");
            return;
        }

        Skill habilidad = p.getPersonaje().getHabilidadesActivas().stream()
                .filter(s -> s.getId().equals(habilidadId))
                .findFirst().orElse(null);

        if (habilidad != null && habilidad.estaLista()) {
            ejecutarEfectoHabilidad(habilidad, p, x, y);
            habilidad.activarCooldown();
            p.setHabilidadUsadaEsteTurno(true);

            // Las habilidades OFENSIVAS sustituyen al disparo normal: cambian el turno.
            // Las DEFENSIVAS se usan como mejora del turno y no lo terminan.
            if (habilidad.getTipo() == SkillType.OFENSIVA) {
                state.cambiarTurno();
            }
        }
        verificarVictoria();
    }

    /** Enrutador de efectos de habilidades activas. */
    private void ejecutarEfectoHabilidad(Skill skill, Player owner, int x, int y) {
        Player enemigo = state.getEnemigo();
        switch (skill.getId()) {
            // --- Wulfrik ---
            case "SKL_WUL_1": ejecutarDesafioErrante(owner, enemigo); break;
            case "SKL_WUL_2": ejecutarColmilloMares(owner, enemigo, x, y); break;
            case "SKL_WUL_3": ejecutarFavorRuinoso(owner); break;
            // --- Aislinn ---
            case "SKL_AIS_1": ejecutarCorteLothern(owner, enemigo); break;
            case "SKL_AIS_2": ejecutarIraMathlann(owner, enemigo, x, y); break;
            case "SKL_AIS_3": ejecutarBrumaMarina(owner); break;
            // --- Lokhir ---
            case "SKL_LOK_1": ejecutarAndanadaDruchii(owner, enemigo, x, y); break;
            case "SKL_LOK_2": ejecutarFuriaCorsaria(enemigo, x, y); break;
            case "SKL_LOK_3": ejecutarYelmoKraken(owner); break;
            // --- Aranessa ---
            case "SKL_ARA_1": ejecutarPolvoraVampirica(owner, enemigo, x, y); break;
            case "SKL_ARA_2": ejecutarDisparoSaloma(owner, enemigo); break;
            case "SKL_ARA_3": ejecutarHijaStromfels(owner); break;
            default: state.setMensajeEstado("Habilidad desconocida: " + skill.getId());
        }
    }

    // --- Wulfrik ---

    /** SKL_WUL_1: Revela (marca en el tablero como REVELADA) la posicion de un BARCO enemigo sin atacarlo. */
    private void ejecutarDesafioErrante(Player owner, Player enemigo) {
        // Buscamos celdas BARCO no reveladas aun
        List<int[]> barcos = celdasConEstado(enemigo.getTablero(), CellStatus.BARCO);
        if (barcos.isEmpty()) { state.setMensajeEstado("No quedan barcos enemigos por descubrir."); return; }
        int[] celda = barcos.get((int)(Math.random() * barcos.size()));
        // Marcar la celda como REVELADA en el tablero del enemigo
        enemigo.getTablero()[celda[0]][celda[1]] = CellStatus.REVELADA;
        state.setMensajeEstado("Desafio del Errante! Barco avistado en (" + celda[0] + "," + celda[1] + ").");
    }

    /** SKL_WUL_2: Dispara a 3 casillas en línea horizontal desde (x,y). */
    private void ejecutarColmilloMares(Player owner, Player enemigo, int x, int y) {
        StringBuilder msg = new StringBuilder("¡Colmillo de los Mares! Impactos: ");
        for (int dy = 0; dy < 3; dy++) {
            int ny = y + dy;
            if (ny < 10) msg.append(aplicarDisparoHabilidad(owner, enemigo, x, ny));
        }
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_WUL_3: Escuda una casilla BARCO propia aleatoria. */
    private void ejecutarFavorRuinoso(Player owner) {
        List<int[]> celdas = celdasConEstado(owner.getTablero(), CellStatus.BARCO);
        celdas.removeIf(c -> owner.tieneEscudo(c[0], c[1]));
        if (celdas.isEmpty()) { state.setMensajeEstado("No hay casillas disponibles para escudar."); return; }
        int[] celda = celdas.get((int)(Math.random() * celdas.size()));
        owner.anadirEscudo(celda[0], celda[1]);
        state.setMensajeEstado("¡Favor Ruinoso! Casilla (" + celda[0] + "," + celda[1] + ") escudada.");
    }

    // --- Aislinn ---

    /** SKL_AIS_1: Dos disparos a casillas enemigas sin atacar, elegidas al azar. */
    private void ejecutarCorteLothern(Player owner, Player enemigo) {
        List<int[]> libres = new ArrayList<>();
        libres.addAll(celdasConEstado(enemigo.getTablero(), CellStatus.BARCO));
        libres.addAll(celdasConEstado(enemigo.getTablero(), CellStatus.AGUA));
        Collections.shuffle(libres);
        StringBuilder msg = new StringBuilder("¡Corte de Lothern! Disparos en: ");
        for (int k = 0; k < Math.min(2, libres.size()); k++) {
            msg.append(aplicarDisparoHabilidad(owner, enemigo, libres.get(k)[0], libres.get(k)[1]));
        }
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_AIS_2: Ataque en cruz de 5 casillas centrado en (x,y). */
    private void ejecutarIraMathlann(Player owner, Player enemigo, int x, int y) {
        int[][] offsets = {{0,0},{-1,0},{1,0},{0,-1},{0,1}};
        StringBuilder msg = new StringBuilder("¡Ira de Mathlann! Cruz en (" + x + "," + y + "): ");
        for (int[] off : offsets) {
            int nx = x + off[0], ny = y + off[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10)
                msg.append(aplicarDisparoHabilidad(owner, enemigo, nx, ny));
        }
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_AIS_3: Escuda 4 casillas BARCO propias aleatorias. */
    private void ejecutarBrumaMarina(Player owner) {
        List<int[]> celdas = celdasConEstado(owner.getTablero(), CellStatus.BARCO);
        celdas.removeIf(c -> owner.tieneEscudo(c[0], c[1]));
        Collections.shuffle(celdas);
        int n = Math.min(4, celdas.size());
        for (int k = 0; k < n; k++) owner.anadirEscudo(celdas.get(k)[0], celdas.get(k)[1]);
        state.setMensajeEstado("¡Bruma Marina! " + n + " casillas protegidas.");
    }

    // --- Lokhir ---

    /** SKL_LOK_1: Dispara a 3 diagonales desde (x,y): NE, SE, SO. */
    private void ejecutarAndanadaDruchii(Player owner, Player enemigo, int x, int y) {
        int[][] diags = {{-1,1},{1,1},{1,-1}};
        StringBuilder msg = new StringBuilder("¡Andanada Druchii! Disparos en: ");
        for (int[] d : diags) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10)
                msg.append(aplicarDisparoHabilidad(owner, enemigo, nx, ny));
        }
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_LOK_2: Revela (marca como REVELADA en tablero) las posiciones de BARCO en area 3x3 centrada en (x,y). */
    private void ejecutarFuriaCorsaria(Player enemigo, int x, int y) {
        StringBuilder reveal = new StringBuilder("Furia Corsaria! Barcos avistados: ");
        boolean encontrado = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10
                        && enemigo.getTablero()[nx][ny] == CellStatus.BARCO) {
                    // Marcar la celda como REVELADA para que el frontend la muestre
                    enemigo.getTablero()[nx][ny] = CellStatus.REVELADA;
                    reveal.append("(").append(nx).append(",").append(ny).append(") ");
                    encontrado = true;
                }
            }
        }
        state.setMensajeEstado(encontrado ? reveal.toString() : "Furia Corsaria! Area despejada.");
    }

    /** SKL_LOK_3: Escuda hasta 5 casillas BARCO propias (da extra vida al barco). */
    private void ejecutarYelmoKraken(Player owner) {
        List<int[]> celdas = celdasConEstado(owner.getTablero(), CellStatus.BARCO);
        celdas.removeIf(c -> owner.tieneEscudo(c[0], c[1]));
        Collections.shuffle(celdas);
        int n = Math.min(5, celdas.size());
        for (int k = 0; k < n; k++) owner.anadirEscudo(celdas.get(k)[0], celdas.get(k)[1]);
        state.setMensajeEstado("¡Yelmo del Kraken! " + n + " casillas reforzadas.");
    }

    // --- Aranessa ---

    /** SKL_ARA_1: Dispara a (x,y); si es BARCO, el fuego se propaga a las 4 casillas adyacentes. */
    private void ejecutarPolvoraVampirica(Player owner, Player enemigo, int x, int y) {
        String res = aplicarDisparoHabilidad(owner, enemigo, x, y);
        CellStatus postImpacto = enemigo.getTablero()[x][y];
        if (postImpacto == CellStatus.TOCADO || postImpacto == CellStatus.HUNDIDO) {
            int[][] adj = {{-1,0},{1,0},{0,-1},{0,1}};
            for (int[] a : adj) {
                int nx = x + a[0], ny = y + a[1];
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10)
                    aplicarDisparoHabilidad(owner, enemigo, nx, ny);
            }
            state.setMensajeEstado("¡Pólvora Vampírica! El fuego se propagó desde (" + x + "," + y + ").");
        } else {
            state.setMensajeEstado("¡Pólvora Vampírica! " + res);
        }
    }

    /** SKL_ARA_2: Dispara a las 4 esquinas del tablero enemigo. */
    private void ejecutarDisparoSaloma(Player owner, Player enemigo) {
        int[][] esquinas = {{0,0},{0,9},{9,0},{9,9}};
        StringBuilder msg = new StringBuilder("¡Disparo de Saloma! Esquinas: ");
        for (int[] e : esquinas) msg.append(aplicarDisparoHabilidad(owner, enemigo, e[0], e[1]));
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_ARA_3: Activa escudo total para el siguiente turno del jugador. */
    private void ejecutarHijaStromfels(Player owner) {
        owner.setEscudoTotalActivo(true);
        state.setMensajeEstado("¡Hija de Stromfels! " + owner.getNombre() + " es invulnerable el próximo turno.");
    }

    // ============================================================
    //  UTILIDADES
    // ============================================================

    /**
     * Aplica daño en (nx,ny) para habilidades activas (respeta escudos y colisiones).
     * Devuelve una cadena descriptiva del resultado para el mensajeEstado.
     */
    private String aplicarDisparoHabilidad(Player owner, Player enemigo, int nx, int ny) {
        CellStatus celda = enemigo.getTablero()[nx][ny];
        if (celda == CellStatus.AGUA_GOLPEADA || celda == CellStatus.TOCADO || celda == CellStatus.HUNDIDO)
            return "(" + nx + "," + ny + ":ya) ";

        if (celda == CellStatus.BARCO || celda == CellStatus.REVELADA) {
            if (enemigo.tieneEscudo(nx, ny)) {
                enemigo.quitarEscudo(nx, ny);
                enemigo.getTablero()[nx][ny] = CellStatus.AGUA_GOLPEADA;
                owner.incrementarHitsFallados();
                return "(" + nx + "," + ny + ":escudo) ";
            }
            enemigo.getTablero()[nx][ny] = CellStatus.TOCADO;
            enemigo.recibirDano();
            owner.incrementarHitsAcertados();
            boolean[] vis = new boolean[100];
            if (dfsSunkCheck(enemigo.getTablero(), nx, ny, vis)) {
                boolean[] visMark = new boolean[100];
                marcarHundido(enemigo.getTablero(), nx, ny, visMark);
                owner.incrementarBarcosHundidos();
                return "(" + nx + "," + ny + ":HUNDIDO) ";
            }
            return "(" + nx + "," + ny + ":tocado) ";
        }
        // AGUA
        enemigo.getTablero()[nx][ny] = CellStatus.AGUA_GOLPEADA;
        owner.incrementarHitsFallados();
        return "(" + nx + "," + ny + ":agua) ";
    }

    /** Devuelve lista de coordenadas del tablero con el estado indicado. */
    private List<int[]> celdasConEstado(CellStatus[][] tablero, CellStatus estado) {
        List<int[]> lista = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 10; j++)
                if (tablero[i][j] == estado) lista.add(new int[]{i, j});
        return lista;
    }

    /** Comprueba si el personaje del jugador tiene la pasiva con el ID dado. */
    private boolean tieneHabilidadPasiva(Player p, String pasivaId) {
        Skill pasiva = p.getPersonaje().getHabilidadPasiva();
        return pasiva != null && pasivaId.equals(pasiva.getId());
    }

    /**
     * Lokhir PAS_LOK: tras hundir un barco en (x,y), revela una celda BARCO adyacente
     * marcandola como REVELADA en el tablero del enemigo y anadiendo sus coordenadas al mensajeEstado.
     */
    private void revelarCeldaAdyacente(Player enemigo, int x, int y) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10
                    && enemigo.getTablero()[nx][ny] == CellStatus.BARCO) {
                // Marcar la celda como REVELADA para que el frontend la muestre visualmente
                enemigo.getTablero()[nx][ny] = CellStatus.REVELADA;
                state.setMensajeEstado(state.getMensajeEstado()
                        + " (Lokhir revela barco en " + nx + "," + ny + ")");
                return;
            }
        }
    }

    // ============================================================
    //  DFS HUNDIR BARCO
    // ============================================================

    private boolean dfsSunkCheck(CellStatus[][] tablero, int x, int y, boolean[] visited) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10) return true;
        int idx = x * 10 + y;
        if (visited[idx]) return true;
        visited[idx] = true;
        CellStatus s = tablero[x][y];
        if (s == CellStatus.AGUA || s == CellStatus.AGUA_GOLPEADA || s == CellStatus.HUNDIDO) return true;
        // BARCO y REVELADA son celdas de barco intactas: el barco aún no está hundido
        if (s == CellStatus.BARCO || s == CellStatus.REVELADA) return false;
        return dfsSunkCheck(tablero, x-1, y, visited)
            && dfsSunkCheck(tablero, x+1, y, visited)
            && dfsSunkCheck(tablero, x, y-1, visited)
            && dfsSunkCheck(tablero, x, y+1, visited);
    }

    private void marcarHundido(CellStatus[][] tablero, int x, int y, boolean[] visited) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10) return;
        int idx = x * 10 + y;
        if (visited[idx]) return;
        visited[idx] = true;
        if (tablero[x][y] == CellStatus.TOCADO) {
            tablero[x][y] = CellStatus.HUNDIDO;
            marcarHundido(tablero, x-1, y, visited);
            marcarHundido(tablero, x+1, y, visited);
            marcarHundido(tablero, x, y-1, visited);
            marcarHundido(tablero, x, y+1, visited);
        }
    }

    // ============================================================
    //  VICTORIA / ESTADO
    // ============================================================

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
        state.setMensajeEstado("¡FIN DE LA PARTIDA!");
    }

    public GameState getState() { return state; }
}
