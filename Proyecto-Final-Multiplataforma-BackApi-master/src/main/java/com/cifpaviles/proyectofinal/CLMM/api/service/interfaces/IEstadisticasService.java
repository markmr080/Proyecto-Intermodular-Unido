package com.cifpaviles.proyectofinal.CLMM.api.service.interfaces;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidasStatsEntity;

public interface IEstadisticasService {

    /**
     * Obtiene las estadísticas del jugador.
     * Si no existe registro previo, crea uno con todos los valores a 0.
     */
    PartidasStatsEntity getStats(String nickname);

    /**
     * Actualiza las estadísticas acumuladas del jugador al finalizar una partida.
     *
     * @param nickname         Nickname del jugador.
     * @param ganada           Si la partida fue ganada.
     * @param impactosAcertados Número de disparos que acertaron en esta partida.
     * @param impactosFallados  Número de disparos que fallaron en esta partida.
     */
    PartidasStatsEntity actualizarStats(String nickname, boolean ganada, int impactosAcertados, int impactosFallados);
}
