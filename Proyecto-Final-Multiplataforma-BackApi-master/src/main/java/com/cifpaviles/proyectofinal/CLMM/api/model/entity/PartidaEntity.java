package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la tabla PARTIDAS (reestructurada).
 * 
 * Cambios respecto a la versión anterior:
 *   - jugador1 (String) → host (ManyToOne FK → USUARIOS)
 *   - Se eliminan: nombreJugador1, avatarJugador1, jugador2, nombreJugador2, avatarJugador2, codigoSala, turno
 *   - Se añaden: ganador (ManyToOne FK → USUARIOS), fechaInicio, fechaFin
 *   - estado: ENUM ampliado con EN_ESPERA y CAIDA_SERVIDOR
 * 
 * La gestión de sala (código de sala, turno) es ahora in-memory via GameState/GameRoomManager.
 * El 2º jugador se referencia desde PARTIDA_STATS (MongoDB).
 */
@Entity
@Table(name = "partidas")
public class PartidaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partida")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_host", nullable = false)
    private UsuarioEntity host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ganador_id")
    private UsuarioEntity ganador;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPartida estado;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // -------------------------------------------------------
    //  Constructores
    // -------------------------------------------------------
    public PartidaEntity() {}

    public PartidaEntity(UsuarioEntity host, EstadoPartida estado) {
        this.host = host;
        this.estado = estado;
        this.fechaInicio = LocalDateTime.now();
    }

    // -------------------------------------------------------
    //  Getters y Setters
    // -------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UsuarioEntity getHost() {
        return host;
    }

    public void setHost(UsuarioEntity host) {
        this.host = host;
    }

    public UsuarioEntity getGanador() {
        return ganador;
    }

    public void setGanador(UsuarioEntity ganador) {
        this.ganador = ganador;
    }

    public EstadoPartida getEstado() {
        return estado;
    }

    public void setEstado(EstadoPartida estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }
}
