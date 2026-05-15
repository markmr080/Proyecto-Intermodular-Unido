package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO recibido al finalizar una partida para guardar las estadísticas en MongoDB.
 * Reestructurado para el nuevo modelo: por partida y con referencia a personaje.
 */
public class ActualizarStatsDTO {

    @NotNull(message = "El id de partida es obligatorio")
    private Long idPartida;

    @NotBlank(message = "El username es obligatorio")
    private String username;

    private Long idPersonaje;

    @Min(value = 0, message = "Los hits acertados no pueden ser negativos")
    private int hitsAcertados;

    @Min(value = 0, message = "Los hits fallados no pueden ser negativos")
    private int hitsFallados;

    @Min(value = 0, message = "Los barcos hundidos no pueden ser negativos")
    private int barcosHundidos;

    // --- Getters y Setters ---

    public Long getIdPartida() { return idPartida; }
    public void setIdPartida(Long idPartida) { this.idPartida = idPartida; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getIdPersonaje() { return idPersonaje; }
    public void setIdPersonaje(Long idPersonaje) { this.idPersonaje = idPersonaje; }

    public int getHitsAcertados() { return hitsAcertados; }
    public void setHitsAcertados(int hitsAcertados) { this.hitsAcertados = hitsAcertados; }

    public int getHitsFallados() { return hitsFallados; }
    public void setHitsFallados(int hitsFallados) { this.hitsFallados = hitsFallados; }

    public int getBarcosHundidos() { return barcosHundidos; }
    public void setBarcosHundidos(int barcosHundidos) { this.barcosHundidos = barcosHundidos; }
}
