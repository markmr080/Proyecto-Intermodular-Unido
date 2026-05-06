package com.cifpaviles.proyectofinal.CLMM.api.model.repository;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.EstadoPartida;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para PartidaEntity.
 * 
 * Cambios respecto a la versión anterior:
 *   - Eliminado: findByCodigoSala (la gestión de sala es in-memory via sockets)
 *   - Añadidos: findByEstado, findByHost
 */
@Repository
public interface PartidaRepository extends JpaRepository<PartidaEntity, Long> {

    /** Obtiene todas las partidas con un estado concreto (ej. EN_ESPERA, EN_CURSO). */
    List<PartidaEntity> findByEstado(EstadoPartida estado);

    /** Obtiene todas las partidas donde el usuario es el host. */
    List<PartidaEntity> findByHost(UsuarioEntity host);

    /** Obtiene todas las partidas donde el usuario fue el ganador. */
    List<PartidaEntity> findByGanador(UsuarioEntity ganador);
}
