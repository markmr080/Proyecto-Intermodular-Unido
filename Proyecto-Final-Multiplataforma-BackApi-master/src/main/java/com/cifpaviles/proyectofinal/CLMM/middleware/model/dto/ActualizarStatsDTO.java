package com.cifpaviles.proyectofinal.CLMM.middleware.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO recibido al finalizar una partida para actualizar las estadísticas del jugador.
 */
public class ActualizarStatsDTO {

    @NotBlank(message = "El nickname es obligatorio")
    private String nickname;

    private boolean ganada;

    @Min(value = 0, message = "Los impactos acertados no pueden ser negativos")
    private int impactosAcertados;

    @Min(value = 0, message = "Los impactos fallados no pueden ser negativos")
    private int impactosFallados;

    // --- Getters y Setters ---

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public boolean isGanada() { return ganada; }
    public void setGanada(boolean ganada) { this.ganada = ganada; }

    public int getImpactosAcertados() { return impactosAcertados; }
    public void setImpactosAcertados(int impactosAcertados) { this.impactosAcertados = impactosAcertados; }

    public int getImpactosFallados() { return impactosFallados; }
    public void setImpactosFallados(int impactosFallados) { this.impactosFallados = impactosFallados; }
}
