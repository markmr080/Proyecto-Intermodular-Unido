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
                // Lokhir: Si el impacto es en el Arca Negra, el resto de escudos caen
                if (enemigo.getPersonaje().getNombre().equals("Lokhir")) {
                    comprobarEscudoArcaNegra(enemigo, x, y);
                }
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
                List<int[]> barcoHundido = findFullShip(enemigo, x, y);
                int tamano = barcoHundido.size();

                boolean[] visMark = new boolean[100];
                marcarHundido(enemigo.getTablero(), x, y, visMark);
                atacante.incrementarBarcosHundidos();
                state.setMensajeEstado("¡" + atacante.getNombre() + " ha HUNDIDO un barco enemigo!");

                // Lokhir: Transformación si pierde el Arca Negra (tamaño 5)
                if (tamano >= 5 && enemigo.getPersonaje().getNombre().equals("Lokhir")) {
                    activarVenganzaLokhir(enemigo);
                }

                // Lokhir PAS_LOK: revela celda adyacente al hundido
                if (tieneHabilidadPasiva(atacante, "PAS_LOK")) {
                    String extra = activarPasivaLokhir(enemigo, x, y);
                    state.setMensajeEstado(state.getMensajeEstado() + extra);
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
            
            // Ikit Claw PAS_IKT: 20% de probabilidad de no consumir enfriamiento en habilidades OFENSIVAS
            boolean bypassCooldown = habilidad.getTipo() == SkillType.OFENSIVA 
                                     && tieneHabilidadPasiva(p, "PAS_IKT") 
                                     && Math.random() < 0.20;
            
            if (!bypassCooldown) {
                habilidad.activarCooldown();
            } else {
                state.setMensajeEstado(state.getMensajeEstado() + " ¡Ingenio Skaven! La habilidad no ha consumido enfriamiento.");
            }

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
            case "SKL_AIS_3": ejecutarBrumaMarina(owner, x, y); break;
            // --- Lokhir ---
            case "SKL_LOK_1": ejecutarAndanadaDruchii(owner, enemigo, x, y); break;
            case "SKL_LOK_2": ejecutarFuriaCorsaria(enemigo, x, y); break;
            case "SKL_LOK_3": ejecutarYelmoKraken(owner); break;
            case "SKL_LOK_4": ejecutarVenganzaKarondKar(owner, enemigo); break;
            // --- Aranessa ---
            case "SKL_ARA_1": ejecutarPolvoraVampirica(owner, enemigo, x, y); break;
            case "SKL_ARA_2": ejecutarDisparoSaloma(owner, enemigo, x, y); break;
            case "SKL_ARA_3": ejecutarHijaStromfels(owner); break;
            // --- Ikit Claw ---
            case "SKL_IKT_1": ejecutarRayoBrujo(owner, enemigo, x, y); break;
            case "SKL_IKT_2": ejecutarCoheteMuerte(owner, enemigo, x, y); break;
            case "SKL_IKT_3": ejecutarEscudoEnergiaBruja(owner, x, y); break;
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

    /** SKL_AIS_3: Escuda un área 2x2 propia. */
    private void ejecutarBrumaMarina(Player owner, int x, int y) {
        if (x < 0 || x > 9 || y < 0 || y > 9) {
            state.setMensajeEstado("Bruma Marina: Las coordenadas de defensa no son válidas.");
            return;
        }
        
        // Aplicar escudos en área 2x2 (o lo que quepa en el tablero)
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 10 && ny < 10) {
                    owner.anadirEscudo(nx, ny);
                }
            }
        }
        state.setMensajeEstado("¡Bruma Marina! Una densa niebla mágica oculta y protege un sector de la flota de " + owner.getNombre() + ".");
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
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                    CellStatus status = enemigo.getTablero()[nx][ny];
                    if (status == CellStatus.BARCO) {
                        enemigo.getTablero()[nx][ny] = CellStatus.REVELADA;
                        encontrado = true;
                    } else if (status == CellStatus.AGUA) {
                        // Revelar también el agua para que el usuario sepa que no hay nada
                        enemigo.getTablero()[nx][ny] = CellStatus.AGUA_GOLPEADA;
                    }
                }
            }
        }
        state.setMensajeEstado(encontrado ? "¡Furia Corsaria! Se han avistado barcos enemigos en el área de exploración." : "¡Furia Corsaria! El área parece estar despejada de enemigos.");
    }

    /**
     * SKL_LOK_3: Yelmo del Kraken.
     * Escuda completamente el barco más grande del jugador (el Arca Negra).
     * El escudo es frágil: si una casilla recibe un impacto, todos los escudos del Arca desaparecen.
     */
    private void ejecutarYelmoKraken(Player owner) {
        List<List<int[]>> barcos = encontrarBarcosCompletos(owner.getTablero());
        if (barcos.isEmpty()) {
            state.setMensajeEstado("No se han encontrado barcos para defender.");
            return;
        }

        // Identificar el barco más grande (Arca Negra)
        List<int[]> arcaNegra = barcos.get(0);
        for (List<int[]> b : barcos) {
            if (b.size() > arcaNegra.size()) arcaNegra = b;
        }

        // Aplicar escudos a todas las celdas del Arca que no estén ya hundidas
        int escudosAplicados = 0;
        for (int[] c : arcaNegra) {
            CellStatus s = owner.getTablero()[c[0]][c[1]];
            if (s == CellStatus.BARCO || s == CellStatus.REVELADA || s == CellStatus.TOCADO) {
                owner.anadirEscudo(c[0], c[1]);
                escudosAplicados++;
            }
        }

        if (escudosAplicados > 0) {
            state.setMensajeEstado("¡Yelmo del Kraken! El navío insignia de Lokhir ha desplegado sus barreras místicas.");
        } else {
            state.setMensajeEstado("El Arca Negra ya ha sido destruida, no hay nada que proteger.");
        }
    }

    /**
     * Activa el modo venganza de Lokhir reemplazando su defensa por un bombardeo ofensivo.
     */
    private void activarVenganzaLokhir(Player owner) {
        owner.getPersonaje().reemplazarHabilidad("SKL_LOK_3", CharacterFactory.crearVenganzaLokhir());
        state.setMensajeEstado(state.getMensajeEstado() + " ¡Lokhir entra en frenesí! El Arca Negra ha caído, pero su furia se desata: Yelmo del Kraken ha sido reemplazado por Venganza de Karond Kar.");
    }

    /**
     * SKL_LOK_4: Venganza de Karond Kar.
     * Lanza 5 disparos aleatorios sobre el tablero enemigo.
     */
    private void ejecutarVenganzaKarondKar(Player owner, Player enemigo) {
        int impactos = 0;
        int hundidos = 0;
        StringBuilder passiveMsg = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            int rx = (int)(Math.random() * 10);
            int ry = (int)(Math.random() * 10);
            String res = aplicarDisparoHabilidad(owner, enemigo, rx, ry);
            if (res.contains("Impacto") || res.contains("HUNDIDO")) {
                impactos++;
                if (res.contains("HUNDIDO")) {
                    hundidos++;
                    if (res.contains("(Lokhir")) {
                        passiveMsg.append(" ").append(res.substring(res.indexOf("(Lokhir")));
                    }
                }
            }
        }

        StringBuilder msg = new StringBuilder("¡VENGANZA DE KAROND KAR! Lokhir desata un bombardeo desesperado.");
        if (impactos > 0) {
            msg.append(" Se han registrado ").append(impactos).append(" impactos.");
            if (hundidos > 0) {
                msg.append(" ¡Y se han HUNDIDO ").append(hundidos).append(" barcos!");
            }
            msg.append(passiveMsg);
        } else {
            msg.append(" El bombardeo ha fallado por completo en medio del caos.");
        }
        state.setMensajeEstado(msg.toString());
    }

    /**
     * Si la casilla (x,y) pertenece al Arca Negra (barco de tamaño 5), 
     * elimina todos los escudos de ese barco.
     */
    private void comprobarEscudoArcaNegra(Player enemigo, int x, int y) {
        List<int[]> barco = findFullShip(enemigo, x, y);
        // Si el barco es de tamaño 5 (Arca Negra), limpiamos sus escudos
        if (barco.size() >= 5) {
            for (int[] c : barco) {
                enemigo.quitarEscudo(c[0], c[1]);
            }
        }
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

    /** SKL_ARA_2: Destruye todos los escudos enemigos y dispara en área 2x2. */
    private void ejecutarDisparoSaloma(Player owner, Player enemigo, int x, int y) {
        boolean teniaEscudos = !enemigo.getEscudoCasillas().isEmpty() || enemigo.isEscudoTotalActivo();
        
        // Limpiar todos los escudos y protecciones del rival
        enemigo.getEscudoCasillas().clear();
        enemigo.setEscudoTotalActivo(false);

        StringBuilder msg = new StringBuilder("¡El rugido de Reina Bess! El Disparo de Saloma ");
        if (teniaEscudos) {
            msg.append("ha DESTROZADO todas las defensas y nieblas enemigas. ");
        }
        
        int impactos = 0;
        // Ataque en área 2x2 y REVELADO de la zona (destruir niebla)
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                    // Revelar primero (destruir niebla)
                    if (enemigo.getTablero()[nx][ny] == CellStatus.BARCO) {
                        enemigo.getTablero()[nx][ny] = CellStatus.REVELADA;
                    } else if (enemigo.getTablero()[nx][ny] == CellStatus.AGUA) {
                        enemigo.getTablero()[nx][ny] = CellStatus.AGUA_GOLPEADA;
                    }
                    
                    String res = aplicarDisparoHabilidad(owner, enemigo, nx, ny);
                    if (res.contains("Impacto") || res.contains("HUNDIDO")) impactos++;
                }
            }
        }
        msg.append(impactos > 0 ? "¡Y ha causado estragos en la flota!" : "El impacto ha levantado una columna de agua gigante.");
        state.setMensajeEstado(msg.toString());
    }

    /** SKL_ARA_3: Activa escudo total para el siguiente turno del jugador. */
    private void ejecutarHijaStromfels(Player owner) {
        owner.setEscudoTotalActivo(true);
        state.setMensajeEstado("¡Hija de Stromfels! La bendición del Dios del Mar hace invulnerable la flota de " + owner.getNombre() + ".");
    }

    // --- Ikit Claw ---

    /** 
     * SKL_IKT_1: Rayo de Piedra Bruja.
     * Impacta en una casilla y revela barcos en las 8 casillas adyacentes.
     * @param owner El jugador que lanza la habilidad.
     * @param enemigo El jugador que recibe el impacto.
     * @param x Coordenada X del impacto.
     * @param y Coordenada Y del impacto.
     */
    private void ejecutarRayoBrujo(Player owner, Player enemigo, int x, int y) {
        String res = aplicarDisparoHabilidad(owner, enemigo, x, y);
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        boolean revelado = false;
        
        // Revelar barcos y agua en forma de cruz (adyacentes directos)
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                CellStatus status = enemigo.getTablero()[nx][ny];
                if (status == CellStatus.BARCO) {
                    enemigo.getTablero()[nx][ny] = CellStatus.REVELADA;
                    revelado = true;
                } else if (status == CellStatus.AGUA) {
                    // El rayo también revela el agua circundante en cruz
                    enemigo.getTablero()[nx][ny] = CellStatus.AGUA_GOLPEADA;
                }
            }
        }

        // Construir mensaje de feedback claro para el usuario
        String baseMsg = "¡Rayo de Piedra Bruja! ";
        if (res.contains("HUNDIDO")) {
            baseMsg += "¡Impacto devastador que ha HUNDIDO un barco enemigo!";
        } else if (res.contains("Impacto")) {
            baseMsg += "Impacto certero en la flota enemiga.";
        } else if (res.contains("Escudo")) {
            baseMsg += "El rayo fue absorbido por un escudo enemigo.";
        } else {
            baseMsg += "El rayo se ha perdido en las profundidades del mar.";
        }

        // Si el resultado contenía un mensaje de pasiva (ej: Lokhir), lo añadimos
        if (res.contains("(Lokhir")) {
            baseMsg += " " + res.substring(res.indexOf("(Lokhir"));
        }

        if (revelado) {
            baseMsg += " El destello ha revelado posiciones enemigas cercanas.";
        }
        
        state.setMensajeEstado(baseMsg);
    }

    /** 
     * SKL_IKT_2: Cohete de Muerte.
     * Impacto masivo en un área de 3x3 casillas.
     * @param owner El jugador que lanza la habilidad.
     * @param enemigo El jugador que recibe el impacto.
     * @param x Centro del área de impacto X.
     * @param y Centro del área de impacto Y.
     */
    private void ejecutarCoheteMuerte(Player owner, Player enemigo, int x, int y) {
        int impactos = 0;
        int hundidos = 0;
        StringBuilder passiveMsg = new StringBuilder();
        
        // Recorrer el área 3x3 centrada en (x,y)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int nx = x + dx, ny = y + dy;
                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                    String res = aplicarDisparoHabilidad(owner, enemigo, nx, ny);
                    if (res.contains("Impacto") || res.contains("HUNDIDO")) {
                        impactos++;
                        if (res.contains("HUNDIDO")) {
                            hundidos++;
                            // Si Lokhir hunde algo con el cohete, añadimos el mensaje
                            if (res.contains("(Lokhir")) {
                                passiveMsg.append(" ").append(res.substring(res.indexOf("(Lokhir")));
                            }
                        }
                    }
                }
            }
        }
        
        // Generar mensaje detallado del bombardeo
        StringBuilder msg = new StringBuilder("¡COHETE DE MUERTE! Una explosión colosal ha sacudido el sector.");
        if (impactos > 0) {
            msg.append(" Se han registrado ").append(impactos).append(" impactos.");
            if (hundidos > 0) {
                msg.append(" ¡Y se han HUNDIDO ").append(hundidos).append(" barcos!");
            }
            // Añadir los mensajes de pasiva si existen
            msg.append(passiveMsg);
        } else {
            msg.append(" Increíblemente, no se han registrado daños directos.");
        }
        
        state.setMensajeEstado(msg.toString());
    }

    /** 
     * SKL_IKT_3: Escudo de Piedra Bruja.
     * Protege un área propia de 2x2 casillas. Si no se especifican coordenadas, 
     * busca automáticamente una zona con barcos.
     * @param owner El jugador que se protege.
     * @param x Esquina superior izquierda del área (o -1 para aleatorio).
     * @param y Esquina superior izquierda del área (o -1 para aleatorio).
     */
    private void ejecutarEscudoEnergiaBruja(Player owner, int x, int y) {
        String subMsg = "";
        // Lógica de selección aleatoria si no hay coordenadas de targeting
        if (x < 0 || x > 8 || y < 0 || y > 8) {
            List<int[]> barcos = celdasConEstado(owner.getTablero(), CellStatus.BARCO);
            if (barcos.isEmpty()) {
                // Si no hay barcos intactos, intentar con los ya dañados
                barcos = celdasConEstado(owner.getTablero(), CellStatus.TOCADO);
            }
            
            if (barcos.isEmpty()) {
                state.setMensajeEstado("Ikit Claw se ríe de la situación: ¡No quedan barcos que proteger!");
                return;
            }
            
            int[] ref = barcos.get((int)(Math.random() * barcos.size()));
            x = Math.max(0, Math.min(8, ref[0]));
            y = Math.max(0, Math.min(8, ref[1]));
            subMsg = " en un sector crítico de la flota.";
        } else {
            subMsg = " en las coordenadas seleccionadas.";
        }

        // Aplicar escudos en el cuadrado 2x2
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                owner.anadirEscudo(x + dx, y + dy);
            }
        }
        
        state.setMensajeEstado("¡Escudo de Piedra Bruja! Un campo de energía verdosa se ha desplegado" + subMsg);
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
                // Lokhir: Si el impacto es en el Arca Negra, el resto de escudos caen
                if (enemigo.getPersonaje().getNombre().equals("Lokhir")) {
                    comprobarEscudoArcaNegra(enemigo, nx, ny);
                }
                // No cambiar el estado de la celda: el barco sigue intacto
                owner.incrementarHitsFallados();
                return "Escudo";
            }
            enemigo.getTablero()[nx][ny] = CellStatus.TOCADO;
            enemigo.recibirDano();
            owner.incrementarHitsAcertados();
            boolean[] vis = new boolean[100];
            if (dfsSunkCheck(enemigo.getTablero(), nx, ny, vis)) {
                List<int[]> barcoHundido = findFullShip(enemigo, nx, ny);
                int tamano = barcoHundido.size();

                boolean[] visMark = new boolean[100];
                marcarHundido(enemigo.getTablero(), nx, ny, visMark);
                owner.incrementarBarcosHundidos();

                // Lokhir: Transformación si pierde el Arca Negra (tamaño 5)
                if (tamano >= 5 && enemigo.getPersonaje().getNombre().equals("Lokhir")) {
                    activarVenganzaLokhir(enemigo);
                }
                
                String msgPasiva = "";
                if (tieneHabilidadPasiva(owner, "PAS_LOK")) {
                    msgPasiva = activarPasivaLokhir(enemigo, nx, ny);
                }
                return "¡HUNDIDO!" + msgPasiva;
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
     * o, si no hay ninguna tocando, revela una aleatoria de la flota enemiga.
     */
    private String activarPasivaLokhir(Player enemigo, int x, int y) {
        // 1. Intentar revelar una adyacente (3x3)
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10
                    && enemigo.getTablero()[nx][ny] == CellStatus.BARCO) {
                enemigo.getTablero()[nx][ny] = CellStatus.REVELADA;
                return " (Lokhir revela barco en " + nx + "," + ny + ")";
            }
        }

        // 2. Si no hay adyacentes, buscar CUALQUIER barco restante
        List<int[]> barcosRestantes = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (enemigo.getTablero()[i][j] == CellStatus.BARCO) {
                    barcosRestantes.add(new int[]{i, j});
                }
            }
        }

        if (!barcosRestantes.isEmpty()) {
            int[] pos = barcosRestantes.get(new java.util.Random().nextInt(barcosRestantes.size()));
            enemigo.getTablero()[pos[0]][pos[1]] = CellStatus.REVELADA;
            return " (Lokhir saquea información: barco revelado en " + pos[0] + "," + pos[1] + ")";
        }

        return "";
    }

    // ============================================================
    //  DFS HUNDIR BARCO
    // ============================================================

    /** Busca el barco completo en la posicion (x,y) sin importar su daño actual. */
    private List<int[]> findFullShip(Player p, int x, int y) {
        List<int[]> ship = new java.util.ArrayList<>();
        boolean[] visited = new boolean[100];
        dfsAnyShipCell(p.getTablero(), x, y, visited, ship);
        return ship;
    }

    private void dfsAnyShipCell(CellStatus[][] tablero, int x, int y, boolean[] visited, List<int[]> grupo) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10) return;
        int idx = x * 10 + y;
        if (visited[idx]) return;
        CellStatus s = tablero[x][y];
        if (s == CellStatus.BARCO || s == CellStatus.REVELADA || s == CellStatus.TOCADO || s == CellStatus.HUNDIDO) {
            visited[idx] = true;
            grupo.add(new int[]{x, y});
            dfsAnyShipCell(tablero, x-1, y, visited, grupo);
            dfsAnyShipCell(tablero, x+1, y, visited, grupo);
            dfsAnyShipCell(tablero, x, y-1, visited, grupo);
            dfsAnyShipCell(tablero, x, y+1, visited, grupo);
        }
    }

    private List<List<int[]>> encontrarBarcosCompletos(CellStatus[][] tablero) {
        boolean[] visited = new boolean[100];
        List<List<int[]>> resultado = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                CellStatus s = tablero[i][j];
                if ((s == CellStatus.BARCO || s == CellStatus.REVELADA || s == CellStatus.TOCADO) && !visited[i * 10 + j]) {
                    List<int[]> grupo = new java.util.ArrayList<>();
                    dfsAnyShipCell(tablero, i, j, visited, grupo);
                    resultado.add(grupo);
                }
            }
        }
        return resultado;
    }

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
