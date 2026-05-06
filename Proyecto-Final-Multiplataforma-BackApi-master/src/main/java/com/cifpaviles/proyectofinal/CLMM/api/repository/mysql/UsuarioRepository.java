package com.cifpaviles.proyectofinal.CLMM.api.repository.mysql;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad UsuarioEntity.
 * Métodos renombrados: findByNickname → findByUsername, existsByNickname → existsByUsername.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<UsuarioEntity> findByUsername(String username);
    Optional<UsuarioEntity> findByEmail(String email);
}
