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

/**
 * Inicializa los datos de referencia en la BD al arrancar.
 *
 * Flotas únicas por personaje (tamaños de cada barco):
 *   Wulfrik  → 5, 4, 3, 3, 2  (flota equilibrada, 17 celdas)
 *   Aislinn  → 5, 4, 3, 2, 2  (flota ágil alta elfa, 16 celdas)
 *   Lokhir   → 5, 3, 3, 2, 2  (flota corsaria rápida, 15 celdas)
 *   Aranessa → 4, 4, 3, 3, 2  (flota pirata pesada, 16 celdas)
 *   Ikit     → 4, 3, 3, 2, 2  (flota ingeniera skaven, 14 celdas)
 *
 * Catálogo de barcos compartido (nombre + tamaño):
 *   Portaaviones=5, Acorazado=4, Crucero=3, Destructor=2, Lancha=1
 */
@Configuration
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner initDatabase(
            BarcosCatalogoRepository barcosRepo,
            PersonajeRepository personajeRepo,
            PersonajeFlotaRepository flotaRepo) {

        return args -> {
            // Siempre recrear el catálogo si está vacío
            if (barcosRepo.count() == 0) {
                barcosRepo.saveAll(Arrays.asList(
                        new BarcosCatalogoEntity("Portaaviones", 5),
                        new BarcosCatalogoEntity("Acorazado",    4),
                        new BarcosCatalogoEntity("Crucero",      3),
                        new BarcosCatalogoEntity("Destructor",   2),
                        new BarcosCatalogoEntity("Lancha",       1)
                ));
                System.out.println("Catálogo de barcos inicializado.");
            }

            if (personajeRepo.count() == 0) {
                // --- Helpers ---
                BarcosCatalogoEntity porta   = barcosRepo.findByNombre("Portaaviones").orElseThrow();
                BarcosCatalogoEntity acoraza = barcosRepo.findByNombre("Acorazado").orElseThrow();
                BarcosCatalogoEntity crucero = barcosRepo.findByNombre("Crucero").orElseThrow();
                BarcosCatalogoEntity destru  = barcosRepo.findByNombre("Destructor").orElseThrow();

                // --- Wulfrik: 5,4,3,3,2 ---
                PersonajeEntity wulfrik = personajeRepo.save(new PersonajeEntity("Wulfrik"));
                flotaRepo.save(new PersonajeFlotaEntity(wulfrik, porta,   1)); // 1× tamaño 5
                flotaRepo.save(new PersonajeFlotaEntity(wulfrik, acoraza, 1)); // 1× tamaño 4
                flotaRepo.save(new PersonajeFlotaEntity(wulfrik, crucero, 2)); // 2× tamaño 3
                flotaRepo.save(new PersonajeFlotaEntity(wulfrik, destru,  1)); // 1× tamaño 2

                // --- Aislinn: 5,4,3,2,2 ---
                PersonajeEntity aislinn = personajeRepo.save(new PersonajeEntity("Aislinn"));
                flotaRepo.save(new PersonajeFlotaEntity(aislinn, porta,   1)); // 1× tamaño 5
                flotaRepo.save(new PersonajeFlotaEntity(aislinn, acoraza, 1)); // 1× tamaño 4
                flotaRepo.save(new PersonajeFlotaEntity(aislinn, crucero, 1)); // 1× tamaño 3
                flotaRepo.save(new PersonajeFlotaEntity(aislinn, destru,  2)); // 2× tamaño 2

                // --- Lokhir: 5,3,3,2,2 ---
                PersonajeEntity lokhir = personajeRepo.save(new PersonajeEntity("Lokhir"));
                flotaRepo.save(new PersonajeFlotaEntity(lokhir, porta,   1)); // 1× tamaño 5
                flotaRepo.save(new PersonajeFlotaEntity(lokhir, crucero, 2)); // 2× tamaño 3
                flotaRepo.save(new PersonajeFlotaEntity(lokhir, destru,  2)); // 2× tamaño 2

                // --- Aranessa: 4,4,3,3,2 ---
                PersonajeEntity aranessa = personajeRepo.save(new PersonajeEntity("Aranessa"));
                flotaRepo.save(new PersonajeFlotaEntity(aranessa, acoraza, 2)); // 2× tamaño 4
                flotaRepo.save(new PersonajeFlotaEntity(aranessa, crucero, 2)); // 2× tamaño 3
                flotaRepo.save(new PersonajeFlotaEntity(aranessa, destru,  1)); // 1× tamaño 2


                System.out.println("Personajes y flotas únicas inicializados.");
            }

            // Asegurar que Ikit Claw existe (por si se añadió después de los demás)
            if (personajeRepo.findByNombre("Ikit Claw").isEmpty()) {
                BarcosCatalogoEntity acoraza = barcosRepo.findByNombre("Acorazado").orElseThrow();
                BarcosCatalogoEntity crucero = barcosRepo.findByNombre("Crucero").orElseThrow();
                BarcosCatalogoEntity destru  = barcosRepo.findByNombre("Destructor").orElseThrow();

                PersonajeEntity ikit = personajeRepo.save(new PersonajeEntity("Ikit Claw"));
                flotaRepo.save(new PersonajeFlotaEntity(ikit, acoraza, 1)); // 1× tamaño 4
                flotaRepo.save(new PersonajeFlotaEntity(ikit, crucero, 2)); // 2× tamaño 3
                flotaRepo.save(new PersonajeFlotaEntity(ikit, destru,  2)); // 2× tamaño 2
                System.out.println("Flota de Ikit Claw inicializada individualmente.");
            }

            // Asegurar que Balthasar Gelt existe
            if (personajeRepo.findByNombre("Balthasar Gelt").isEmpty()) {
                BarcosCatalogoEntity porta   = barcosRepo.findByNombre("Portaaviones").orElseThrow();
                BarcosCatalogoEntity acoraza = barcosRepo.findByNombre("Acorazado").orElseThrow();
                BarcosCatalogoEntity crucero = barcosRepo.findByNombre("Crucero").orElseThrow();
                BarcosCatalogoEntity destru  = barcosRepo.findByNombre("Destructor").orElseThrow();

                PersonajeEntity gelt = personajeRepo.save(new PersonajeEntity("Balthasar Gelt"));
                flotaRepo.save(new PersonajeFlotaEntity(gelt, porta,   1)); // 1× tamaño 5
                flotaRepo.save(new PersonajeFlotaEntity(gelt, acoraza, 1)); // 1× tamaño 4
                flotaRepo.save(new PersonajeFlotaEntity(gelt, crucero, 2)); // 2× tamaño 3
                flotaRepo.save(new PersonajeFlotaEntity(gelt, destru,  1)); // 1× tamaño 2
                System.out.println("Flota de Balthasar Gelt inicializada individualmente.");
            }
        };
    }
}
