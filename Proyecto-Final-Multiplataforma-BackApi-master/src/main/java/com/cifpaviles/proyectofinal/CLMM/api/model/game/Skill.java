package com.cifpaviles.proyectofinal.CLMM.api.model.game;

/**
 * Representa una habilidad activa o pasiva de un personaje.
 */
public class Skill {
    private String id;
    private String nombre;
    private String descripcion;
    private SkillType tipo;
    
    // GestiÃ³n de enfriamiento (Cooldown) basado en turnos
    private int cooldownMax;    // CuÃ¡ntos turnos hay que esperar tras usarla
    private int cooldownActual; // CuÃ¡ntos turnos faltan actualmente (0 = lista)

    public Skill(String id, String nombre, String descripcion, SkillType tipo, int cooldownMax) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.cooldownMax = cooldownMax;
        this.cooldownActual = 0; // Empieza lista para usar
    }

    /**
     * Verifica si la habilidad se puede usar.
     */
    public boolean estaLista() {
        return cooldownActual == 0;
    }

    /**
     * Se llama cuando el jugador usa la habilidad.
     */
    public void activarCooldown() {
        this.cooldownActual = this.cooldownMax;
    }

    /**
     * Se debe llamar cada vez que el turno del jugador termina 
     * o comienza (segÃºn decidas la lÃ³gica).
     */
    public void reducirCooldown() {
        if (this.cooldownActual > 0) {
            this.cooldownActual--;
        }
    }

    // --- Getters y Setters ---

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public SkillType getTipo() { return tipo; }
    public int getCooldownActual() { return cooldownActual; }
    public int getCooldownMax() { return cooldownMax; }

    public void setCooldownActual(int cooldownActual) { 
        this.cooldownActual = cooldownActual; 
    }
}
