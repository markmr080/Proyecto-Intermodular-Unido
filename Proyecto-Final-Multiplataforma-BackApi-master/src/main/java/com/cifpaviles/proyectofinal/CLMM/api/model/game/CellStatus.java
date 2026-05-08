package com.cifpaviles.proyectofinal.CLMM.api.model.game;

/**
 * Representa los posibles estados de una casilla individual en el tablero.
 */
public enum CellStatus {
    /**
     * Estado inicial: La casilla es mar y no se sabe quÃ© hay ahÃ­.
     */
    AGUA,

    /**
     * Hay un barco en esta posiciÃ³n, pero el enemigo aÃºn no lo sabe.
     */
    BARCO,

    /**
     * El jugador ha disparado aquÃ­ y habÃ­a un barco. Â¡Impacto!
     */
    TOCADO,

    /**
     * El jugador disparÃ³ aquÃ­ pero solo habÃ­a agua. 
     * Sirve para que el tablero sepa que ya se intentÃ³ atacar esta casilla.
     */
    AGUA_GOLPEADA,

    /**
     * Opcional: Si todas las casillas de un barco estÃ¡n en estado TOCADO, 
     * puedes cambiarlas a HUNDIDO para efectos visuales o lÃ³gicos.
     */
    HUNDIDO,

    /**
     * El jugador sabe que aquí hay un barco (revelado por habilidad de visión)
     * pero no ha disparado sobre él: el barco está intacto.
     * Referencias: SKL_WUL_1 (Desafío del Errante), SKL_LOK_2 (Furia Corsaria),
     *              PAS_LOK (Saqueador Especialista).
     */
    REVELADA
}
