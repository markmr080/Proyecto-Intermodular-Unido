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
 *   - PAS_AIS (Aislinn): 20% de ignorar escudos enemigos al disparar
 *   - PAS_LOK (Lokhir): al hundir, revela una celda BARCO adyacente del enemigo
 *   - PAS_ARA (Aranessa): 20% de esquivar un impacto entrante (Tripulación de los Muertos)
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

        // Aislinn PAS_AIS: 20% de probabilidad de ignorar escudos
        boolean ignoraEscudos = tieneHabilidadPasiva(atacante, "PAS_AIS") && Math.random() < 0.20;

        // Aranessa SKL_ARA_3: escudo total activo — el disparo falla automáticamente
        if (enemigo.isEscudoTotalActivo() && !ignoraEscudos) {
            enemigo.setEscudoTotalActivo(false);
            atacante.incrementarHitsFallados();
            state.setMensajeEstado("¡El Escudo de Stromfels protegió a " + enemigo.getNombre() + "! Disparo bloqueado.");
            state.cambiarTurno();
            return;
        } else if (enemigo.isEscudoTotalActivo() && ignoraEscudos) {
            state.setMensajeEstado("¡Vientos de Magia! El disparo de Aislinn atraviesa el escudo total.");
        }

        if (celda == CellStatus.BARCO || celda == CellStatus.REVELADA) {
            // Wulfrik SKL_WUL_3 / Aislinn SKL_AIS_3: escudo de casilla.
            if (enemigo.tieneEscudo(x, y) && !ignoraEscudos) {
                enemigo.quitarEscudo(x, y);
                atacante.incrementarHitsFallados();
                state.setMensajeEstado("¡Escudo! El impacto de " + atacante.getNombre() + " fue absorbido. La celda sigue en pie.");
                state.cambiarTurno();
                return;
            } else if (enemigo.tieneEscudo(x, y) && ignoraEscudos) {
                state.setMensajeEstado("¡Vientos de Magia! El disparo de Aislinn ignora el escudo de la casilla.");
            }

            // Aranessa PAS_ARA: 20% de esquivar (Tripulación de los Muertos)
            if (tieneHabilidadPasiva(enemigo, "PAS_ARA") && Math.random() < 0.20) {
                atacante.incrementarHitsFallados();
                state.setMensajeEstado("¡La Tripulación de los Muertos mantiene el barco a flote! Aranessa ignoró el impacto.");
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
                state.setMensajeEstado("¡" + atacante.getNombre() + " ha HUNDIDO un barco enemigo!");
                // Lokhir PAS_LOK: revela celda adyacente al hundido
                if (tieneHabilidadPasiva(atacante, "PAS_LOK")) {
                    revelarCeldaAdyacente(enemigo, x, y);
                }
            } else {
                state.setMensajeEstado("¡Impacto certero de " + atacante.getNombre() + "!");
            }

            // Wulfrik PAS_WUL: disparo extra en el mismo turno (solo una vez)
            if (tieneHabilidadPasiva(atacante, "PAS_WUL") && !eraDisparoExtra) {
                atacante.setTurnoExtraWulfrik(true);
                atacante.setHaAtacadoEsteTurno(false); // permitir siguiente disparo
                state.setMensajeEstado(state.getMensajeEstado() + " ¡Wulfrik se prepara para disparar de nuevo!");
                verificarVictoria();
                return; // NO cambia el turno todavía
            }

        } else if (celda == CellStatus.AGUA) {
            enemigo.getTablero()[x][y] = CellStatus.AGUA_GOLPEADA;
            atacante.incrementarHitsFallados();
            state.setMensajeEstado(atacante.getNombre() + " ha disparado al agua.");
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
            case "SKL_WUL_1": ejecutarDesafioErrante(owner, enemigo, x, y); break;
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
            case "SKL_ARA_2": ejecutarDisparoSaloma(owner, enemigo, x, y); break;
            case "SKL_ARA_3": ejecutarHijaStromfels(owner); break;
            default: state.setMensajeEstado("Habilidad desconocida: " + skill.getId());
        }
    }

    // --- Wulfrik ---

    /** SKL_WUL_1: Dispara a (x,y); si falla, revela la posicion de un BARCO enemigo aleatorio. */
    private void ejecutarDesafioErrante(Player owner, Player enemigo, int x, int y) {
        // Validación de coordenadas para evitar crash si el frontend no las envía
        if (x < 0 || x >= 10 || y < 0 || y >= 10) {
            state.setMensajeEstado("¡Desafío del Errante! El disparo se perdió en la niebla.");
            return;
        }

        String res = aplicarDisparoHabilidad(owner, enemigo, x, y);
        CellStatus postImpacto = enemigo.getTablero()[x][y];

        // Solo si el disparo cae en agua (falla), se activa el revelado
        if (postImpacto == CellStatus.AGUA_GOLPEADA) {
            List<int[]> barcos = celdasConEstado(enemigo.getTablero(), CellStatus.BARCO);
            if (!barcos.isEmpty()) {
                int[] celda = barcos.get((int) (Math.random() * barcos.size()));
                enemigo.getTablero()[celda[0]][celda[1]] = CellStatus.REVELADA;
                state.setMensajeEstado("¡Desafío del Errante! Tras fallar el tiro, " + owner.getNombre() + " ha avistado un barco enemigo.");
            } else {
                state.setMensajeEstado("¡Desafío del Errante! " + res + " No se han detectado más enemigos.");
            }
        } else {
            state.setMensajeEstado("¡Desafío del Errante! ¡Impacto directo!");
        }
    }

    /** SKL_WUL_2: Dispara a 3 casillas en línea horizontal desde (x,y). */
    private void ejecutarColmilloMares(Player owner, Player enemigo, int x, int y) {
        StringBuilder msg = new StringBuilder("¡Colmillo de los Mares! ");
        List<String> resultados = new ArrayList<>();
        // Rango corregido de 0..2 a -1..1 para que sea centrado
        for (int dy = -1; dy <= 1; dy++) {
            int ny = y + dy;
            if (ny >= 0 && ny < 10) {
                resultados.add(aplicarDisparoHabilidad(owner, enemigo, x, ny));
            }
        }
        
        // Formatear mensaje descriptivo
        long impactos = resultados.stream().filter(r -> r.contains("Impacto") || r.contains("HUNDIDO")).count();
        if (impactos > 0) {
            msg.append("¡Varios impactos detectados en la zona!");
        } else {
            msg.append("Los disparos solo han levantado espuma.");
        }
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_WUL_3: Escuda una casilla BARCO propia aleatoria. */
    private void ejecutarFavorRuinoso(Player owner) {
        List<int[]> celdas = celdasConEstado(owner.getTablero(), CellStatus.BARCO);
        celdas.removeIf(c -> owner.tieneEscudo(c[0], c[1]));
        if (celdas.isEmpty()) { state.setMensajeEstado("No quedan casillas para proteger."); return; }
        int[] celda = celdas.get((int)(Math.random() * celdas.size()));
        owner.anadirEscudo(celda[0], celda[1]);
        // Mensaje sin coordenadas para no revelar la posición al enemigo
        state.setMensajeEstado("¡Favor Ruinoso! Los Dioses del Caos han protegido una de las casillas de " + owner.getNombre() + ".");
    }

    // --- Aislinn ---

    /** SKL_AIS_1: Dos disparos a casillas enemigas sin atacar, elegidas al azar. */
    private void ejecutarCorteLothern(Player owner, Player enemigo) {
        List<int[]> libres = new ArrayList<>();
        libres.addAll(celdasConEstado(enemigo.getTablero(), CellStatus.BARCO));
        libres.addAll(celdasConEstado(enemigo.getTablero(), CellStatus.AGUA));
        Collections.shuffle(libres);
        StringBuilder msg = new StringBuilder("¡Corte de Lothern! Las flechas mágicas de Aislinn ");
        int impactos = 0;
        for (int k = 0; k < Math.min(2, libres.size()); k++) {
            String res = aplicarDisparoHabilidad(owner, enemigo, libres.get(k)[0], libres.get(k)[1]);
            if (res.contains("Impacto") || res.contains("HUNDIDO")) impactos++;
        }
        msg.append(impactos > 0 ? "han alcanzado objetivos enemigos." : "se han perdido en el mar.");
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_AIS_2: Ataque en cruz de 5 casillas centrado en (x,y). */
    private void ejecutarIraMathlann(Player owner, Player enemigo, int x, int y) {
        int[][] offsets = {{0,0},{-1,0},{1,0},{0,-1},{0,1}};
        StringBuilder msg = new StringBuilder("¡Ira de Mathlann! Una tormenta mágica ");
        int impactos = 0;
        for (int[] off : offsets) {
            int nx = x + off[0], ny = y + off[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                String res = aplicarDisparoHabilidad(owner, enemigo, nx, ny);
                if (res.contains("Impacto") || res.contains("HUNDIDO")) impactos++;
            }
        }
        msg.append(impactos > 0 ? "ha golpeado con fuerza la flota enemiga." : "ha pasado de largo sin causar daños.");
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_AIS_3: Escuda 4 casillas BARCO propias aleatorias. */
    private void ejecutarBrumaMarina(Player owner) {
        List<int[]> celdas = celdasConEstado(owner.getTablero(), CellStatus.BARCO);
        celdas.removeIf(c -> owner.tieneEscudo(c[0], c[1]));
        Collections.shuffle(celdas);
        int n = Math.min(4, celdas.size());
        for (int k = 0; k < n; k++) owner.anadirEscudo(celdas.get(k)[0], celdas.get(k)[1]);
        state.setMensajeEstado("¡Bruma Marina! Una densa niebla mágica oculta y protege los barcos de " + owner.getNombre() + ".");
    }

    // --- Lokhir ---

    /** SKL_LOK_1: Dispara a 3 diagonales desde (x,y): NE, SE, SO. */
    private void ejecutarAndanadaDruchii(Player owner, Player enemigo, int x, int y) {
        int[][] diags = {{-1,1},{1,1},{1,-1}};
        StringBuilder msg = new StringBuilder("¡Andanada Druchii! Una ráfaga de proyectiles oscuros ");
        int impactos = 0;
        for (int[] d : diags) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                String res = aplicarDisparoHabilidad(owner, enemigo, nx, ny);
                if (res.contains("Impacto") || res.contains("HUNDIDO")) impactos++;
            }
        }
        msg.append(impactos > 0 ? "ha destrozado posiciones enemigas." : "ha caído inútilmente al mar.");
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_LOK_2: Revela (marca como REVELADA en tablero) las posiciones de BARCO en area 3x3 centrada en (x,y). */
    private void ejecutarFuriaCorsaria(Player enemigo, int x, int y) {
        boolean encontrado = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10
                        && enemigo.getTablero()[nx][ny] == CellStatus.BARCO) {
                    // Marcar la celda como REVELADA para que el frontend la muestre
                    enemigo.getTablero()[nx][ny] = CellStatus.REVELADA;
                    encontrado = true;
                }
            }
        }
        state.setMensajeEstado(encontrado ? "¡Furia Corsaria! Se han avistado barcos enemigos en el área de exploración." : "¡Furia Corsaria! El área parece estar despejada de enemigos.");
    }

    /**
     * SKL_LOK_3: Reubica aleatoriamente uno de los barcos intactos del jugador.
     * Pasos: (1) localiza todos los grupos de celdas BARCO conectadas (barcos),
     * (2) elige uno al azar, (3) borra esas celdas del tablero,
     * (4) intenta colocar el barco en una nueva posicion aleatoria valida.
     * Si no encuentra hueco libre tras 200 intentos, devuelve el barco a su posicion original.
     */
    private void ejecutarYelmoKraken(Player owner) {
        List<List<int[]>> barcos = encontrarBarcos(owner.getTablero());
        if (barcos.isEmpty()) {
            state.setMensajeEstado("No quedan barcos intactos para reubicar.");
            return;
        }

        // Elegir un barco al azar y recordar su posicion original
        List<int[]> barco = barcos.get((int)(Math.random() * barcos.size()));
        int size = barco.size();

        // Borrar el barco del tablero (y sus escudos si los tuviera)
        for (int[] c : barco) {
            owner.getTablero()[c[0]][c[1]] = CellStatus.AGUA;
            owner.quitarEscudo(c[0], c[1]);
        }

        // Buscar nueva posicion valida
        boolean colocado = false;
        int intentos = 0;
        while (!colocado && intentos < 200) {
            intentos++;
            boolean horizontal = Math.random() > 0.5 || size == 1;
            int startX, startY;
            if (horizontal) {
                startX = (int)(Math.random() * 10);
                startY = (int)(Math.random() * (10 - size + 1));
            } else {
                startX = (int)(Math.random() * (10 - size + 1));
                startY = (int)(Math.random() * 10);
            }

            // Calcular las celdas que ocuparia
            List<int[]> nuevasCeldas = new ArrayList<>();
            for (int k = 0; k < size; k++) {
                int nx = horizontal ? startX : startX + k;
                int ny = horizontal ? startY + k : startY;
                nuevasCeldas.add(new int[]{nx, ny});
            }

            // Verificar que no hay barcos adyacentes (incluye diagonales)
            boolean valido = true;
            outer:
            for (int[] c : nuevasCeldas) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        int ax = c[0] + dx, ay = c[1] + dy;
                        if (ax >= 0 && ax < 10 && ay >= 0 && ay < 10) {
                            CellStatus cs = owner.getTablero()[ax][ay];
                            if (cs == CellStatus.BARCO) { valido = false; break outer; }
                        }
                    }
                }
            }

            if (valido) {
                for (int[] c : nuevasCeldas) owner.getTablero()[c[0]][c[1]] = CellStatus.BARCO;
                colocado = true;
            }
        }

        if (!colocado) {
            // Sin hueco libre: restaurar en su posicion original
            for (int[] c : barco) owner.getTablero()[c[0]][c[1]] = CellStatus.BARCO;
            state.setMensajeEstado("¡Yelmo del Kraken! No hay espacio libre. El barco permanece en su lugar.");
        } else {
            state.setMensajeEstado("¡Yelmo del Kraken! Un barco ha sido reubicado a una nueva posicion.");
        }
    }

    /**
     * Encuentra todos los grupos de celdas BARCO conectadas (barcos intactos) en el tablero.
     * Usa DFS para agrupar celdas adyacentes (sin diagonales) con estado BARCO.
     * Referencia: ejecutarYelmoKraken (SKL_LOK_3).
     */
    private List<List<int[]>> encontrarBarcos(CellStatus[][] tablero) {
        boolean[] visited = new boolean[100];
        List<List<int[]>> resultado = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (tablero[i][j] == CellStatus.BARCO && !visited[i * 10 + j]) {
                    List<int[]> grupo = new ArrayList<>();
                    dfsBarco(tablero, i, j, visited, grupo);
                    resultado.add(grupo);
                }
            }
        }
        return resultado;
    }

    /**
     * DFS auxiliar de encontrarBarcos: recorre celdas BARCO conectadas (4-direccional).
     */
    private void dfsBarco(CellStatus[][] tablero, int x, int y, boolean[] visited, List<int[]> grupo) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10) return;
        int idx = x * 10 + y;
        if (visited[idx] || tablero[x][y] != CellStatus.BARCO) return;
        visited[idx] = true;
        grupo.add(new int[]{x, y});
        dfsBarco(tablero, x - 1, y, visited, grupo);
        dfsBarco(tablero, x + 1, y, visited, grupo);
        dfsBarco(tablero, x, y - 1, visited, grupo);
        dfsBarco(tablero, x, y + 1, visited, grupo);
    }

    // --- Aranessa ---

    /** SKL_ARA_1: Dispara a (x,y); si es BARCO, el fuego se propaga a las 4 casillas adyacentes. */
    private void ejecutarPolvoraVampirica(Player owner, Player enemigo, int x, int y) {
        String res = aplicarDisparoHabilidad(owner, enemigo, x, y);
        if (res.contains("Impacto") || res.contains("HUNDIDO")) {
            int[][] adj = {{-1,0},{1,0},{0,-1},{0,1}};
            int impactosExtra = 0;
            for (int[] a : adj) {
                int nx = x + a[0], ny = y + a[1];
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                    String rExtra = aplicarDisparoHabilidad(owner, enemigo, nx, ny);
                    if (rExtra.contains("Impacto") || rExtra.contains("HUNDIDO")) impactosExtra++;
                }
            }
            state.setMensajeEstado("¡Pólvora Vampírica! El impacto inicial ha propagado el fuego a barcos cercanos.");
        } else {
            state.setMensajeEstado("¡Pólvora Vampírica! El disparo solo ha salpicado de brea el agua.");
        }
    }

    /** SKL_ARA_2: Destruye escudos enemigos y dispara en área 2x2 centrada en (x,y). */
    private void ejecutarDisparoSaloma(Player owner, Player enemigo, int x, int y) {
        // "Destruye forzosamente nieblas o escudos": limpiar todos los escudos del enemigo
        enemigo.getEscudoCasillas().clear();
        enemigo.setEscudoTotalActivo(false);

        StringBuilder msg = new StringBuilder("¡El rugido de Reina Bess! El disparo de saloma ");
        int impactos = 0;
        // Ataque en área 2x2 (cuadrante inferior derecho desde x,y)
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                    String res = aplicarDisparoHabilidad(owner, enemigo, nx, ny);
                    if (res.contains("Impacto") || res.contains("HUNDIDO")) impactos++;
                }
            }
        }
        msg.append(impactos > 0 ? "ha devastado la zona de impacto." : "ha fallado por poco.");
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_ARA_3: Activa escudo total para el siguiente turno del jugador. */
    private void ejecutarHijaStromfels(Player owner) {
        owner.setEscudoTotalActivo(true);
        state.setMensajeEstado("¡Hija de Stromfels! La bendición del Dios del Mar hace invulnerable la flota de " + owner.getNombre() + ".");
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
            return "Repetido";

        if (celda == CellStatus.BARCO || celda == CellStatus.REVELADA) {
            // Escudo de casilla: la celda permanece BARCO, el escudo se consume, sin daño
            if (enemigo.tieneEscudo(nx, ny)) {
                enemigo.quitarEscudo(nx, ny);
                // No cambiar el estado de la celda: el barco sigue intacto
                owner.incrementarHitsFallados();
                return "Escudo";
            }
            enemigo.getTablero()[nx][ny] = CellStatus.TOCADO;
            enemigo.recibirDano();
            owner.incrementarHitsAcertados();
            boolean[] vis = new boolean[100];
            if (dfsSunkCheck(enemigo.getTablero(), nx, ny, vis)) {
                boolean[] visMark = new boolean[100];
                marcarHundido(enemigo.getTablero(), nx, ny, visMark);
                owner.incrementarBarcosHundidos();
                return "¡HUNDIDO!";
            }
            return "Impacto";
        }
        // AGUA
        enemigo.getTablero()[nx][ny] = CellStatus.AGUA_GOLPEADA;
        owner.incrementarHitsFallados();
        return "Agua";
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
