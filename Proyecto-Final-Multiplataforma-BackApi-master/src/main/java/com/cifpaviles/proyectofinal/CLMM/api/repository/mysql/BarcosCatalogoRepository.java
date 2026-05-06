package com.cifpaviles.proyectofinal.CLMM.api.repository.mysql;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.BarcosCatalogoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad BarcosCatalogoEntity (tabla BARCOS_CATALOGO).
 * Reemplaza al antiguo BarcoRepository.
 */
@Repository
public interface BarcosCatalogoRepository extends JpaRepository<BarcosCatalogoEntity, Long> {

    Optional<BarcosCatalogoEntity> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
