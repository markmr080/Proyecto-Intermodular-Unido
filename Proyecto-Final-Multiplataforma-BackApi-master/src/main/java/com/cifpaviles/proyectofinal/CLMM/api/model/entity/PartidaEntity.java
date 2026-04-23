package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "partidas")
public class PartidaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "jugador1")
    private String jugador1;

    @Column(name = "nombre_jugador1")
    private String nombreJugador1;

    @Column(name = "avatar_jugador1")
    private String avatarJugador1;

    @Column(name = "jugador2")
    private String jugador2;

    @Column(name = "nombre_jugador2")
    private String nombreJugador2;

    @Column(name = "avatar_jugador2")
    private String avatarJugador2;

    @Column(name = "estado", nullable = false)
    private EstadoPartida estado;

    @Column(name = "codigo_sala", unique = true)
    private String codigoSala;

    @Column(name = "turno")
    private String turno;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getJugador1() {
        return jugador1;
    }

    public void setJugador1(String jugador1) {
        this.jugador1 = jugador1;
    }

    public String getNombreJugador1() {
        return nombreJugador1;
    }

    public void setNombreJugador1(String nombreJugador1) {
        this.nombreJugador1 = nombreJugador1;
    }

    public String getAvatarJugador1() {
        return avatarJugador1;
    }

    public void setAvatarJugador1(String avatarJugador1) {
        this.avatarJugador1 = avatarJugador1;
    }

    public String getJugador2() {
        return jugador2;
    }

    public void setJugador2(String jugador2) {
        this.jugador2 = jugador2;
    }

    public String getNombreJugador2() {
        return nombreJugador2;
    }

    public void setNombreJugador2(String nombreJugador2) {
        this.nombreJugador2 = nombreJugador2;
    }

    public String getAvatarJugador2() {
        return avatarJugador2;
    }

    public void setAvatarJugador2(String avatarJugador2) {
        this.avatarJugador2 = avatarJugador2;
    }

    public EstadoPartida getEstado() {
        return estado;
    }

    public void setEstado(EstadoPartida estado) {
        this.estado = estado;
    }

    public String getCodigoSala() {
        return codigoSala;
    }

    public void setCodigoSala(String codigoSala) {
        this.codigoSala = codigoSala;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }
}
