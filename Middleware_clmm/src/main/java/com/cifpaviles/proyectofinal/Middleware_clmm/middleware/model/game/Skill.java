package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.game;

public class Skill {
    private String id;
    private String nombre;
    private String descripcion;
    private SkillType tipo;
    private String icono;
    private int cooldownMax;
    private int cooldownActual;

    public Skill(String id, String nombre, String descripcion, SkillType tipo, int cooldownMax, String icono) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.cooldownMax = cooldownMax;
        this.icono = icono;
        this.cooldownActual = 0;
    }

    public boolean estaLista() {
        return cooldownActual == 0;
    }

    public void activarCooldown() {
        this.cooldownActual = this.cooldownMax;
    }

    public void reducirCooldown() {
        if (this.cooldownActual > 0) {
            this.cooldownActual--;
        }
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public SkillType getTipo() { return tipo; }
    public String getIcono() { return icono; }
    public int getCooldownActual() { return cooldownActual; }
    public int getCooldownMax() { return cooldownMax; }
    public void setCooldownActual(int cooldownActual) { this.cooldownActual = cooldownActual; }
}
