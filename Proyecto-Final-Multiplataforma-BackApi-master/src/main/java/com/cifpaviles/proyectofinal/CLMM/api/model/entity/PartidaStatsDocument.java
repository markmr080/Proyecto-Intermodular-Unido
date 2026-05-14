package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento MongoDB que representa las estadísticas de UN jugador en UNA partida.
 * Colección: partida_stats
 * 
 * Cada vez que finaliza una partida, se insertan TANTOS documentos como jugadores participaron.
 * Patrón: insert-only (nunca se actualiza un documento existente).
 * 
 * El campo 'username' se desnormaliza para poder hacer queries de ranking
 * sin necesidad de hacer JOIN contra MySQL.
 */
@Document(collection = "partida_stats")
public class PartidaStatsDocument {

    @Id
    private String id;  // ObjectId de MongoDB (generado automáticamente)

    /** Referencia a PARTIDAS.id_partida (MySQL) */
    private Long idPartida;

    /** Referencia a USUARIOS.id_usuario (MySQL) */
    private Long idUsuario;

    /** Referencia a PERSONAJES.id_personaje (MySQL) */
    private Long idPersonaje;

    private int hitsAcertados = 0;
    private int hitsFallados = 0;
    private int barcosHundidos = 0;
    private boolean ganador = false;

    /** Desnormalizado para queries rápidas de ranking sin JOIN a MySQL */
    private String username;

    // -------------------------------------------------------
    //  Constructores
    // -------------------------------------------------------
    public PartidaStatsDocument() {}

    public PartidaStatsDocument(Long idPartida, Long idUsuario, Long idPersonaje,
                                 int hitsAcertados, int hitsFallados, int barcosHundidos,
                                 String username, boolean ganador) {
        this.idPartida = idPartida;
        this.idUsuario = idUsuario;
        this.idPersonaje = idPersonaje;
        this.hitsAcertados = hitsAcertados;
        this.hitsFallados = hitsFallados;
        this.barcosHundidos = barcosHundidos;
        this.username = username;
        this.ganador = ganador;
    }

    // -------------------------------------------------------
    //  Getters y Setters
    // -------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }


    public Long getIdPartida() { return idPartida; }
    public void setIdPartida(Long idPartida) { this.idPartida = idPartida; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public Long getIdPersonaje() { return idPersonaje; }
    public void setIdPersonaje(Long idPersonaje) { this.idPersonaje = idPersonaje; }

    public int getHitsAcertados() { return hitsAcertados; }
    public void setHitsAcertados(int hitsAcertados) { this.hitsAcertados = hitsAcertados; }

    public int getHitsFallados() { return hitsFallados; }
    public void setHitsFallados(int hitsFallados) { this.hitsFallados = hitsFallados; }

    public int getBarcosHundidos() { return barcosHundidos; }
    public void setBarcosHundidos(int barcosHundidos) { this.barcosHundidos = barcosHundidos; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isGanador() { return ganador; }
    public void setGanador(boolean ganador) { this.ganador = ganador; }

    // -------------------------------------------------------

    //  Utilidad: porcentaje de puntería calculado al vuelo
    // -------------------------------------------------------
    public String getPunteria() {
        int total = hitsAcertados + hitsFallados;
        if (total == 0) return "0%";
        return String.format("%.1f%%", (hitsAcertados * 100.0) / total);
    }
}
