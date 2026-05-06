package com.cifpaviles.proyectofinal.CLMM.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuración dedicada para JPA/MySQL.
 * Asegura que JPA solo escanea los repositorios MySQL, sin confundirse con los de MongoDB.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
        "com.cifpaviles.proyectofinal.CLMM.api.repository.mysql",
        "com.cifpaviles.proyectofinal.CLMM.api.model.repository"
})
public class JpaConfig {
    // Asegurar que JPA solo escanea repositorios MySQL
}
