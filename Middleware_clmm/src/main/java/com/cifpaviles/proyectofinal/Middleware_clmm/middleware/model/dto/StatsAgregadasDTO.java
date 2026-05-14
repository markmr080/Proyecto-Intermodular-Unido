package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto;

/**
 * DTO que representa las estadísticas agregadas de un jugador
 * a partir de todos sus documentos en MongoDB (una entrada por partida).
 */
public class StatsAgregadasDTO {

    private String username;
    private int partidasJugadas;
    private int partidasGanadas;
    private int hitsAcertados;
    private int hitsFallados;
    private int barcosHundidos;
    private String punteria;  // Calculado: hitsAcertados / (hitsAcertados + hitsFallados) * 100

    public StatsAgregadasDTO() {}

    public StatsAgregadasDTO(String username, int partidasJugadas, int partidasGanadas,
                              int hitsAcertados, int hitsFallados, int barcosHundidos) {
        this.username = username;
        this.partidasJugadas = partidasJugadas;
        this.partidasGanadas = partidasGanadas;
        this.hitsAcertados = hitsAcertados;
        this.hitsFallados = hitsFallados;
        this.barcosHundidos = barcosHundidos;
        // Calcular puntería
        int total = hitsAcertados + hitsFallados;
        this.punteria = total == 0 ? "0%" : String.format("%.1f%%", (hitsAcertados * 100.0) / total);
    }

    // -------------------------------------------------------
    //  Getters y Setters
    // -------------------------------------------------------

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getPartidasJugadas() { return partidasJugadas; }
    public void setPartidasJugadas(int partidasJugadas) { this.partidasJugadas = partidasJugadas; }

    public int getPartidasGanadas() { return partidasGanadas; }
    public void setPartidasGanadas(int partidasGanadas) { this.partidasGanadas = partidasGanadas; }

    public int getHitsAcertados() { return hitsAcertados; }
    public void setHitsAcertados(int hitsAcertados) { this.hitsAcertados = hitsAcertados; }

    public int getHitsFallados() { return hitsFallados; }
    public void setHitsFallados(int hitsFallados) { this.hitsFallados = hitsFallados; }

    public int getBarcosHundidos() { return barcosHundidos; }
    public void setBarcosHundidos(int barcosHundidos) { this.barcosHundidos = barcosHundidos; }

    public String getPunteria() { return punteria; }
    public void setPunteria(String punteria) { this.punteria = punteria; }
}
