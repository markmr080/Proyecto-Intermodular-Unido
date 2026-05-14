package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameCharacter {
    private String nombre;
    private String descripcion;
    private String imagen;
    private Skill habilidadPasiva;
    private List<Skill> habilidadesActivas;
    private Map<String, Integer> flotaPermitida;

    public GameCharacter(String nombre, String descripcion, String imagen, Skill pasiva) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.imagen = imagen;
        this.habilidadPasiva = pasiva;
        this.habilidadesActivas = new ArrayList<>();
        this.flotaPermitida = new HashMap<>();
    }

    public void aplicarEfectoPasivo(GameState estado, Player dueno) {}

    public void anadirHabilidadActiva(Skill skill) {
        this.habilidadesActivas.add(skill);
    }

    public void anadirBarcoAFlota(String tipoBarco, int cantidad) {
        this.flotaPermitida.put(tipoBarco, cantidad);
    }

    public void reemplazarHabilidad(String idAntiguo, Skill nuevaSkill) {
        for (int i = 0; i < habilidadesActivas.size(); i++) {
            if (habilidadesActivas.get(i).getId().equals(idAntiguo)) {
                habilidadesActivas.set(i, nuevaSkill);
                return;
            }
        }
    }

    public int getTotalHealth() {
        java.util.Map<String, Integer> tamanos = java.util.Map.of(
            "Portaaviones", 5, "Acorazado", 4, "Crucero", 3, "Destructor", 2, "Submarino", 2, "Lancha", 1
        );
        int total = 0;
        for (java.util.Map.Entry<String, Integer> entry : flotaPermitida.entrySet()) {
            total += tamanos.getOrDefault(entry.getKey(), 0) * entry.getValue();
        }
        return total;
    }

    public java.util.List<Integer> getFlotaComoListaTamanos() {
        java.util.Map<String, Integer> tamanos = new java.util.HashMap<>();
        tamanos.put("Portaaviones", 5);
        tamanos.put("Acorazado",    4);
        tamanos.put("Crucero",      3);
        tamanos.put("Destructor",   2);
        tamanos.put("Submarino",   2);
        tamanos.put("Lancha",       1);

        java.util.List<Integer> lista = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : flotaPermitida.entrySet()) {
            int tam = tamanos.getOrDefault(entry.getKey(), 0);
            for (int i = 0; i < entry.getValue(); i++) {
                lista.add(tam);
            }
        }
        lista.sort(java.util.Comparator.reverseOrder());
        return lista;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getImagen() { return imagen; }
    public Skill getHabilidadPasiva() { return habilidadPasiva; }
    public List<Skill> getHabilidadesActivas() { return habilidadesActivas; }
    public Map<String, Integer> getFlotaPermitida() { return flotaPermitida; }
}
