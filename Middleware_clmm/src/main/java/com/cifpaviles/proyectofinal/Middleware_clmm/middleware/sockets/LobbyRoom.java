package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.sockets;

public class LobbyRoom {
    private String codigoSala;
    private String jugador1; // ID
    private String nombreJugador1;
    private String avatarJugador1;
    private String jugador2; // ID
    private String nombreJugador2;
    private String avatarJugador2;
    private String estado;
    private long fechaCreacion;
    private Long idPartidaMysql;

    public LobbyRoom() {
        this.fechaCreacion = System.currentTimeMillis();
    }

    public String getCodigoSala() { return codigoSala; }
    public void setCodigoSala(String codigoSala) { this.codigoSala = codigoSala; }

    public String getJugador1() { return jugador1; }
    public void setJugador1(String jugador1) { this.jugador1 = jugador1; }

    public String getNombreJugador1() { return nombreJugador1; }
    public void setNombreJugador1(String nombreJugador1) { this.nombreJugador1 = nombreJugador1; }

    public String getAvatarJugador1() { return avatarJugador1; }
    public void setAvatarJugador1(String avatarJugador1) { this.avatarJugador1 = avatarJugador1; }

    public String getJugador2() { return jugador2; }
    public void setJugador2(String jugador2) { this.jugador2 = jugador2; }

    public String getNombreJugador2() { return nombreJugador2; }
    public void setNombreJugador2(String nombreJugador2) { this.nombreJugador2 = nombreJugador2; }

    public String getAvatarJugador2() { return avatarJugador2; }
    public void setAvatarJugador2(String avatarJugador2) { this.avatarJugador2 = avatarJugador2; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public long getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(long fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Long getIdPartidaMysql() { return idPartidaMysql; }
    public void setIdPartidaMysql(Long idPartidaMysql) { this.idPartidaMysql = idPartidaMysql; }
}
