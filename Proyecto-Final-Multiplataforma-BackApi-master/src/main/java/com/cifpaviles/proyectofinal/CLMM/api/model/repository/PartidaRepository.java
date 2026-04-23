package com.cifpaviles.proyectofinal.CLMM.api.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity;

@Repository
public interface PartidaRepository extends JpaRepository<PartidaEntity, Long> {
    Optional<PartidaEntity> findByCodigoSala(String codigoSala);
}
