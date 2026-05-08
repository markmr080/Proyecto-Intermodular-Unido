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
public class GameCharacter {
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
    public void aplicarEfectoPasivo(GameState estado, Player dueno) {}

    public void anadirHabilidadActiva(Skill skill) {
        this.habilidadesActivas.add(skill);
    }

    public void anadirBarcoAFlota(String tipoBarco, int cantidad) {
        this.flotaPermitida.put(tipoBarco, cantidad);
    }

    /**
     * Devuelve la flota como lista de tamaños de celda ordenada de mayor a menor.
     * El tamaño se infiere del nombre estándar del barco.
     * Usado por el frontend para saber cuántos barcos y de qué tamaño colocar.
     */
    public java.util.List<Integer> getFlotaComoListaTamanos() {
        // Mapa nombre de barco → tamaño (debe coincidir con barcos_catalogo)
        java.util.Map<String, Integer> tamanos = new java.util.HashMap<>();
        tamanos.put("Portaaviones", 5);
        tamanos.put("Acorazado",    4);
        tamanos.put("Crucero",      3);
        tamanos.put("Destructor",   2);
        tamanos.put("Lancha",       1);

        java.util.List<Integer> lista = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : flotaPermitida.entrySet()) {
            int tam = tamanos.getOrDefault(entry.getKey(), 0);
            for (int i = 0; i < entry.getValue(); i++) {
                lista.add(tam);
            }
        }
        // Ordenar de mayor a menor para que la UI los muestre en orden lógico
        lista.sort(java.util.Comparator.reverseOrder());
        return lista;
    }

    // --- Getters y Setters ---

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Skill getHabilidadPasiva() { return habilidadPasiva; }
    public List<Skill> getHabilidadesActivas() { return habilidadesActivas; }
    public Map<String, Integer> getFlotaPermitida() { return flotaPermitida; }
}
