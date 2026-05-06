package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

/**
 * Enum que representa los posibles estados de una partida.
 * Se añade CAIDA_SERVIDOR y se renombra ESPERANDO → EN_ESPERA.
 */
public enum EstadoPartida {
    EN_ESPERA,
    EN_CURSO,
    FINALIZADA,
    CAIDA_SERVIDOR
}
