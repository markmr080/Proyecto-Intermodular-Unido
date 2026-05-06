package com.cifpaviles.proyectofinal.CLMM.api.repository.mysql;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad PersonajeEntity (tabla PERSONAJES).
 */
@Repository
public interface PersonajeRepository extends JpaRepository<PersonajeEntity, Long> {

    Optional<PersonajeEntity> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
