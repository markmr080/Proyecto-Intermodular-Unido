package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "partidas_stats")
public class PartidasStatsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false)
    private int partidasGanadas = 0;

    @Column(nullable = false)
    private int impactosAcertados = 0;

    @Column(nullable = false)
    private int impactosFallados = 0;

    // -------------------------------------------------------
    //  Constructor sin parámetros requerido por JPA/Hibernate
    // -------------------------------------------------------
    public PartidasStatsEntity() {}

    // -------------------------------------------------------
    //  Constructor de conveniencia: crea un registro nuevo a 0
    //  para un jugador que todavía no tiene estadísticas
    // -------------------------------------------------------
    public PartidasStatsEntity(String nickname) {
        this.nickname = nickname;
        this.partidasGanadas = 0;
        this.impactosAcertados = 0;
        this.impactosFallados = 0;
    }

    // -------------------------------------------------------
    //  Getters y Setters
    // -------------------------------------------------------

    public Long getId() { return id; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public int getPartidasGanadas() { return partidasGanadas; }
    public void setPartidasGanadas(int partidasGanadas) { this.partidasGanadas = partidasGanadas; }

    public int getImpactosAcertados() { return impactosAcertados; }
    public void setImpactosAcertados(int impactosAcertados) { this.impactosAcertados = impactosAcertados; }

    public int getImpactosFallados() { return impactosFallados; }
    public void setImpactosFallados(int impactosFallados) { this.impactosFallados = impactosFallados; }

    // -------------------------------------------------------
    //  Utilidad: porcentaje de puntería calculado al vuelo
    // -------------------------------------------------------
    public String getPunteria() {
        int total = impactosAcertados + impactosFallados;
        if (total == 0) return "0%";
        return String.format("%.1f%%", (impactosAcertados * 100.0) / total);
    }
}
