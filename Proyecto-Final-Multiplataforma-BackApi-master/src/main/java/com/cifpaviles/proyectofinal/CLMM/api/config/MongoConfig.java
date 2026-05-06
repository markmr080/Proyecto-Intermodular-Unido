package com.cifpaviles.proyectofinal.CLMM.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Configuración dedicada para MongoDB.
 * Indica a Spring Data que los repositorios Mongo están en el paquete 'mongo'.
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.cifpaviles.proyectofinal.CLMM.api.repository.mongo")
public class MongoConfig {
    // Configuración dedicada para MongoDB - sin conflicto con JPA
}
