package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.interfaces;

public interface IGameServiceImpl {
    /**
     * Aquí irán los métodos que gestionarás por WebSockets más adelante.
     */
    void procesarDisparo(String jugador, int x, int y);
    void iniciarPartida(String jugador1, String jugador2);
}
