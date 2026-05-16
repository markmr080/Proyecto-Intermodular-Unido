package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.game;

import java.util.HashSet;
import java.util.Set;

public class Player {
    private String id;
    private String nombre;
    private GameCharacter personaje;
    private CellStatus[][] tablero;
    private int vidas;
    private boolean habilidadUsadaEsteTurno;
    private boolean haAtacadoEsteTurno;
    private boolean turnoExtraWulfrik = false;
    private Set<String> escudoCasillas = new HashSet<>();
    private Set<String> celdasBrumaMarina = new HashSet<>();
    private boolean escudoTotalActivo = false;
    private boolean listoParaCombate = false;
    private int hitsAcertados = 0;
    private int hitsFallados = 0;
    private int barcosHundidos = 0;

    public Player(String id, String nombre, GameCharacter personaje) {
        this.id = id;
        this.nombre = nombre;
        this.personaje = personaje;
        if (personaje != null) {
            this.vidas = personaje.getTotalHealth();
        }
        this.tablero = new CellStatus[10][10];
        this.habilidadUsadaEsteTurno = false;
        this.haAtacadoEsteTurno = false;
        inicializarTablero();
    }

    private void inicializarTablero() {
        for (int i = 0; i < 10; i++)
            for (int j = 0; j < 10; j++)
                tablero[i][j] = CellStatus.AGUA;
    }

    public boolean estaVivo() { return this.vidas > 0; }
    public void recibirDano() { if (this.vidas > 0) this.vidas--; }
    public void anadirEscudo(int x, int y) { escudoCasillas.add(x + "," + y); }
    public boolean tieneEscudo(int x, int y) { return escudoCasillas.contains(x + "," + y); }
    public void quitarEscudo(int x, int y) { 
        escudoCasillas.remove(x + "," + y); 
        celdasBrumaMarina.remove(x + "," + y);
    }
    
    public void anadirBrumaMarina(int x, int y) {
        String coord = x + "," + y;
        escudoCasillas.add(coord);
        celdasBrumaMarina.add(coord);
    }

    public void limpiarBrumaMarina() {
        for (String coord : celdasBrumaMarina) {
            escudoCasillas.remove(coord);
        }
        celdasBrumaMarina.clear();
    }

    public boolean esParteDeBrumaMarina(int x, int y) {
        return celdasBrumaMarina.contains(x + "," + y);
    }
 
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public GameCharacter getPersonaje() { return personaje; }
    public void setPersonaje(GameCharacter personaje) { 
        this.personaje = personaje; 
        if (personaje != null) {
            this.vidas = personaje.getTotalHealth();
        }
    }
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
