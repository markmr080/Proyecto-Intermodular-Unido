package com.cifpaviles.proyectofinal.CLMM.api.model.game;

/**
 * Representa a un jugador en la partida.
 * Contiene su tablero personal, su personaje y sus estadÃ­sticas.
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

    // Nuevo campo para saber si ha terminado de colocar barcos
    private boolean listoParaCombate = false;

    public Player(String id, String nombre, GameCharacter personaje) {
        this.id = id;
        this.nombre = nombre;
        this.personaje = personaje;
        this.tablero = new CellStatus[10][10];
        this.habilidadUsadaEsteTurno = false;
        this.haAtacadoEsteTurno = false;
        
        // Inicializar el tablero lleno de agua por defecto
        inicializarTablero();
    }

    /**
     * Llena la matriz inicial con el estado AGUA.
     */
    private void inicializarTablero() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                tablero[i][j] = CellStatus.AGUA;
            }
        }
    }

    /**
     * Verifica si al jugador aÃºn le quedan barcos a flote.
     */
    public boolean estaVivo() {
        return this.vidas > 0;
    }

    /**
     * Resta vida cuando un barco es golpeado.
     */
    public void recibirDano() {
        if (this.vidas > 0) {
            this.vidas--;
        }
    }

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
    public void setHabilidadUsadaEsteTurno(boolean estado) { 
        this.habilidadUsadaEsteTurno = estado; 
    }

    public boolean isHaAtacadoEsteTurno() { return haAtacadoEsteTurno; }
    public void setHaAtacadoEsteTurno(boolean estado) { 
        this.haAtacadoEsteTurno = estado; 
    }
}
