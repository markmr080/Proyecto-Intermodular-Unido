package com.cifpaviles.proyectofinal.CLMM.api.model.game;

import java.util.HashSet;
import java.util.Set;

/**
 * Representa a un jugador en la partida.
 * Contiene su tablero personal, su personaje y sus estadísticas.
 * Referencias: GameEngine (lógica de disparo), GameState (estado global).
 */
public class Player {
    private String id;
    private String nombre;
    private GameCharacter personaje;
    private CellStatus[][] tablero; // Matriz de 10x10
    private int vidas; // Total de celdas de barco que le quedan

    // Control de reglas por turno
    private boolean habilidadUsadaEsteTurno;
    private boolean haAtacadoEsteTurno;

    // Wulfrik: disparo extra al acertar (pasiva)
    private boolean turnoExtraWulfrik = false;

    // Escudos en casillas específicas (Wulfrik SKL_WUL_3, Aislinn SKL_AIS_3, Lokhir SKL_LOK_3)
    // El set almacena coordenadas en formato "x,y"
    private Set<String> escudoCasillas = new HashSet<>();

    // Aranessa SKL_ARA_3: escudo total durante un turno completo
    private boolean escudoTotalActivo = false;

    // Nuevo campo para saber si ha terminado de colocar barcos
    private boolean listoParaCombate = false;

    // Estadísticas
    private int hitsAcertados = 0;
    private int hitsFallados = 0;
    private int barcosHundidos = 0;

    public Player(String id, String nombre, GameCharacter personaje) {
        this.id = id;
        this.nombre = nombre;
        this.personaje = personaje;
        this.tablero = new CellStatus[10][10];
        this.habilidadUsadaEsteTurno = false;
        this.haAtacadoEsteTurno = false;
        inicializarTablero();
    }

    /** Llena la matriz inicial con el estado AGUA. */
    private void inicializarTablero() {
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 10; j++)
                tablero[i][j] = CellStatus.AGUA;
    }

    /** Verifica si al jugador aún le quedan barcos a flote. */
    public boolean estaVivo() { return this.vidas > 0; }

    /** Resta vida cuando un barco es golpeado. */
    public void recibirDano() { if (this.vidas > 0) this.vidas--; }

    // --- Escudos de casilla ---

    /** Añade un escudo a la casilla indicada. El próximo impacto en ella fallará. */
    public void anadirEscudo(int x, int y) { escudoCasillas.add(x + "," + y); }

    /** Comprueba si la casilla tiene un escudo activo. */
    public boolean tieneEscudo(int x, int y) { return escudoCasillas.contains(x + "," + y); }

    /** Elimina el escudo de la casilla (se consume al usarse). */
    public void quitarEscudo(int x, int y) { escudoCasillas.remove(x + "," + y); }

    // --- Getters y Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public GameCharacter getPersonaje() { return personaje; }
    public CellStatus[][] getTablero() { return tablero; }
    public void setTablero(CellStatus[][] tablero) { this.tablero = tablero; }

    public int getVidas() { return vidas; }
    public void setVidas(int vidas) { this.vidas = vidas; }

    public boolean isListoParaCombate() { return this.listoParaCombate; }
    public void setListoParaCombate(boolean listo) { this.listoParaCombate = listo; }

    public boolean isHabilidadUsadaEsteTurno() { return habilidadUsadaEsteTurno; }
    public void setHabilidadUsadaEsteTurno(boolean estado) { this.habilidadUsadaEsteTurno = estado; }

    public boolean isHaAtacadoEsteTurno() { return haAtacadoEsteTurno; }
    public void setHaAtacadoEsteTurno(boolean estado) { this.haAtacadoEsteTurno = estado; }

    public boolean isTurnoExtraWulfrik() { return turnoExtraWulfrik; }
    public void setTurnoExtraWulfrik(boolean turnoExtraWulfrik) { this.turnoExtraWulfrik = turnoExtraWulfrik; }

    public Set<String> getEscudoCasillas() { return escudoCasillas; }

    public boolean isEscudoTotalActivo() { return escudoTotalActivo; }
    public void setEscudoTotalActivo(boolean escudoTotalActivo) { this.escudoTotalActivo = escudoTotalActivo; }

    public int getHitsAcertados() { return hitsAcertados; }
    public void incrementarHitsAcertados() { this.hitsAcertados++; }

    public int getHitsFallados() { return hitsFallados; }
    public void incrementarHitsFallados() { this.hitsFallados++; }

    public int getBarcosHundidos() { return barcosHundidos; }
    public void incrementarBarcosHundidos() { this.barcosHundidos++; }
}
