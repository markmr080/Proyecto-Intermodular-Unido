package com.cifpaviles.proyectofinal.CLMM.api.config;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.BarcosCatalogoEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeFlotaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.BarcosCatalogoRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeFlotaRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner initDatabase(
            BarcosCatalogoRepository barcosRepo,
            PersonajeRepository personajeRepo,
            PersonajeFlotaRepository flotaRepo) {

        return args -> {
            // Inicializar Barcos
            if (barcosRepo.count() == 0) {
                barcosRepo.saveAll(Arrays.asList(
                        new BarcosCatalogoEntity("Portaaviones", 5),
                        new BarcosCatalogoEntity("Acorazado", 4),
                        new BarcosCatalogoEntity("Crucero", 3),
                        new BarcosCatalogoEntity("Submarino", 2)
                ));
                System.out.println("Catálogo de barcos inicializado.");
            }

            // Inicializar Personajes
            if (personajeRepo.count() == 0) {
                List<String> nombresPersonajes = Arrays.asList("Wulfrik", "Aislinn", "Lokhir", "Aranessa");
                for (String nombre : nombresPersonajes) {
                    PersonajeEntity p = new PersonajeEntity(nombre);
                    personajeRepo.save(p);

                    // Asignar flota por defecto a cada personaje
                    BarcosCatalogoEntity portaaviones = barcosRepo.findByNombre("Portaaviones").orElseThrow();
                    BarcosCatalogoEntity acorazado = barcosRepo.findByNombre("Acorazado").orElseThrow();
                    BarcosCatalogoEntity crucero = barcosRepo.findByNombre("Crucero").orElseThrow();
                    BarcosCatalogoEntity submarino = barcosRepo.findByNombre("Submarino").orElseThrow();

                    flotaRepo.save(new PersonajeFlotaEntity(p, portaaviones, 1));
                    flotaRepo.save(new PersonajeFlotaEntity(p, acorazado, 2));
                    flotaRepo.save(new PersonajeFlotaEntity(p, crucero, 3));
                    flotaRepo.save(new PersonajeFlotaEntity(p, submarino, 4));
                }
                System.out.println("Personajes y flotas inicializados.");
            }
        };
    }
}
