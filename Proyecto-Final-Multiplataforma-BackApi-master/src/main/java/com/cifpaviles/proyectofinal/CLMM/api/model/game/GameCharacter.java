package com.cifpaviles.proyectofinal.CLMM.api.model.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase base para todos los personajes del juego.
 * Cada personaje tiene una pasiva Ãºnica y un conjunto de habilidades activas.
 */
public abstract class GameCharacter {
    private String nombre;
    private String descripcion;
    private Skill habilidadPasiva;
    private List<Skill> habilidadesActivas;

    public GameCharacter(String nombre, String descripcion, Skill pasiva) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.habilidadPasiva = pasiva;
        this.habilidadesActivas = new ArrayList<>();
    }

    /**
     * Este mÃ©todo es el "corazÃ³n" de la lÃ³gica personalizada.
     * Cada personaje concreto (ej. Capitan) dirÃ¡ quÃ© hace su pasiva aquÃ­.
     * * @param estado El estado global del juego para poder modificarlo.
     * @param dueno El jugador que posee a este personaje.
     */
    public abstract void aplicarEfectoPasivo(GameState estado, Player dueno);

    public void anadirHabilidadActiva(Skill skill) {
        this.habilidadesActivas.add(skill);
    }

    // --- Getters y Setters ---

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Skill getHabilidadPasiva() { return habilidadPasiva; }
    public List<Skill> getHabilidadesActivas() { return habilidadesActivas; }
}
