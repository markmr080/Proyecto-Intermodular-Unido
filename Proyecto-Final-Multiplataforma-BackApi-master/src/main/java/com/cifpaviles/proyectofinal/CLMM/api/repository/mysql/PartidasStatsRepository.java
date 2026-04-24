package com.cifpaviles.proyectofinal.CLMM.api.repository.mysql;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidasStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartidasStatsRepository extends JpaRepository<PartidasStatsEntity, Long> {

    Optional<PartidasStatsEntity> findByNickname(String nickname);

    boolean existsByNickname(String nickname);
}
