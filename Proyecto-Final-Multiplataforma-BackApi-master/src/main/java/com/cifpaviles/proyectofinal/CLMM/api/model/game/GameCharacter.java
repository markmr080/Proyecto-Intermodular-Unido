package com.cifpaviles.proyectofinal.CLMM.api.model.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase base para todos los personajes del juego.
 * Cada personaje tiene una pasiva única, un conjunto de habilidades activas
 * y una flota predefinida (leída de la base de datos).
 */
public abstract class GameCharacter {
    private String nombre;
    private String descripcion;
    private Skill habilidadPasiva;
    private List<Skill> habilidadesActivas;
    
    // Mapa: Tipo de Barco -> Cantidad permitida
    private Map<String, Integer> flotaPermitida;

    public GameCharacter(String nombre, String descripcion, Skill pasiva) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.habilidadPasiva = pasiva;
        this.habilidadesActivas = new ArrayList<>();
        this.flotaPermitida = new HashMap<>();
    }

    /**
     * Este método es el "corazón" de la lógica personalizada.
     * Cada personaje concreto (ej. Capitan) dirá qué hace su pasiva aquí.
     * @param estado El estado global del juego para poder modificarlo.
     * @param dueno El jugador que posee a este personaje.
     */
    public abstract void aplicarEfectoPasivo(GameState estado, Player dueno);

    public void anadirHabilidadActiva(Skill skill) {
        this.habilidadesActivas.add(skill);
    }

    public void anadirBarcoAFlota(String tipoBarco, int cantidad) {
        this.flotaPermitida.put(tipoBarco, cantidad);
    }

    // --- Getters y Setters ---

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Skill getHabilidadPasiva() { return habilidadPasiva; }
    public List<Skill> getHabilidadesActivas() { return habilidadesActivas; }
    public Map<String, Integer> getFlotaPermitida() { return flotaPermitida; }
}
