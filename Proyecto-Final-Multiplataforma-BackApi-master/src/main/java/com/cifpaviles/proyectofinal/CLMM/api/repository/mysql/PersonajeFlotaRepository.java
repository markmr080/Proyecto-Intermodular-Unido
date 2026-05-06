package com.cifpaviles.proyectofinal.CLMM.api.repository.mysql;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeFlotaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeFlotaId;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad PersonajeFlotaEntity (tabla PERSONAJE_FLOTA).
 * Usa la clave compuesta PersonajeFlotaId.
 */
@Repository
public interface PersonajeFlotaRepository extends JpaRepository<PersonajeFlotaEntity, PersonajeFlotaId> {

    /** Obtiene todos los barcos de la flota de un personaje. */
    List<PersonajeFlotaEntity> findByPersonaje(PersonajeEntity personaje);

    /** Obtiene todos los barcos de la flota de un personaje por su id. */
    List<PersonajeFlotaEntity> findByPersonaje_Id(Long idPersonaje);
}
