package com.cifpaviles.proyectofinal.CLMM.api.repository.mongo;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaStatsDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio MongoDB para la colección 'partida_stats'.
 * Reemplaza al antiguo PartidasStatsRepository (MySQL).
 */
@Repository
public interface EstadisticasRepository extends MongoRepository<PartidaStatsDocument, String> {

    /** Obtiene todas las entradas de stats de un usuario (historial de partidas). */
    List<PartidaStatsDocument> findByIdUsuario(Long idUsuario);

    /** Obtiene todas las entradas de stats de una partida concreta. */
    List<PartidaStatsDocument> findByIdPartida(Long idPartida);

    /** Obtiene stats por username (campo desnormalizado para queries rápidas). */
    List<PartidaStatsDocument> findByUsername(String username);
}
