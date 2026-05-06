package com.cifpaviles.proyectofinal.CLMM.api.service.interfaces;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaStatsDocument;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.StatsAgregadasDTO;

import java.util.List;

/**
 * Interfaz del servicio de estadísticas (ahora sobre MongoDB).
 */
public interface IEstadisticasService {

    /**
     * Obtiene las estadísticas agregadas de un jugador (todas sus partidas sumadas).
     *
     * @param username Username del jugador.
     * @return DTO con totales acumulados.
     */
    StatsAgregadasDTO getStatsAgregadas(String username);

    /**
     * Guarda las estadísticas de una partida finalizada para un jugador.
     *
     * @param idPartida      Id de la partida en MySQL.
     * @param idUsuario      Id del usuario en MySQL.
     * @param idPersonaje    Id del personaje usado (puede ser null si no aplica).
     * @param hitsAcertados  Disparos que acertaron.
     * @param hitsFallados   Disparos que fallaron.
     * @param barcosHundidos Barcos hundidos en esta partida.
     * @param username       Username desnormalizado para queries rápidas.
     * @return El documento guardado.
     */
    PartidaStatsDocument guardarStatsPartida(Long idPartida, Long idUsuario, Long idPersonaje,
                                              int hitsAcertados, int hitsFallados, int barcosHundidos,
                                              String username);

    /**
     * Obtiene el historial de partidas (documentos individuales) de un jugador.
     *
     * @param idUsuario Id del usuario.
     * @return Lista de documentos de stats.
     */
    List<PartidaStatsDocument> getHistorial(Long idUsuario);
}
