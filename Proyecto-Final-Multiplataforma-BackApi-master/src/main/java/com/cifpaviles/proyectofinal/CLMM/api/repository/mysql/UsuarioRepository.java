package com.cifpaviles.proyectofinal.CLMM.api.repository.mysql;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    boolean existsByEmail(String Email);
    boolean existsByNickname(String nombre);
    Optional<UsuarioEntity> findByNickname(String nombre);
    Optional<UsuarioEntity> findByEmail(String email);

}
