package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeFlotaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.game.*;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeFlotaRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de instanciar personajes específicos con sus habilidades.
 * Ahora consulta a la base de datos (PERSONAJES y PERSONAJE_FLOTA) para configurar
 * la flota de cada personaje.
 */
@Service
public class CharacterFactory {

    private final PersonajeRepository personajeRepository;
    private final PersonajeFlotaRepository personajeFlotaRepository;

    public CharacterFactory(PersonajeRepository personajeRepository, PersonajeFlotaRepository personajeFlotaRepository) {
        this.personajeRepository = personajeRepository;
        this.personajeFlotaRepository = personajeFlotaRepository;
    }

    /**
     * Crea un personaje basado en su nombre o ID.
     */
    public GameCharacter crearPersonaje(String tipo) {
        GameCharacter baseCharacter;
        switch (tipo.toUpperCase()) {
            case "ARTILLERO":
                baseCharacter = crearArtillero();
                break;
            case "COMANDANTE":
                baseCharacter = crearComandante();
                break;
            default:
                baseCharacter = crearArtillero(); // Personaje por defecto
        }

        // Leer la flota desde la BD
        Optional<PersonajeEntity> optPersonaje = personajeRepository.findByNombre(baseCharacter.getNombre());
        if (optPersonaje.isPresent()) {
            List<PersonajeFlotaEntity> flota = personajeFlotaRepository.findByPersonaje(optPersonaje.get());
            for (PersonajeFlotaEntity pf : flota) {
                baseCharacter.anadirBarcoAFlota(pf.getBarcoTipo().getNombre(), pf.getCantidad());
            }
        } else {
            // Si no está en BD (por ej. tests o no inicializado), añadimos una flota por defecto
            baseCharacter.anadirBarcoAFlota("Portaaviones", 1);
            baseCharacter.anadirBarcoAFlota("Acorazado", 2);
            baseCharacter.anadirBarcoAFlota("Crucero", 3);
            baseCharacter.anadirBarcoAFlota("Submarino", 4);
        }

        return baseCharacter;
    }

    private GameCharacter crearArtillero() {
        Skill pasiva = new Skill("PAS_01", "Puntería", "Aumenta la probabilidad de crítico", SkillType.PASIVA, 0);

        GameCharacter artillero = new GameCharacter("Artillero", "Experto en ataques múltiples.", pasiva) {
            @Override
            public void aplicarEfectoPasivo(GameState estado, Player dueno) {
                // Lógica de la pasiva: Se ejecuta automáticamente.
            }
        };

        artillero.anadirHabilidadActiva(new Skill(
            "SKL_OFF_01", 
            "Ráfaga", 
            "Dispara 4 veces en posiciones aleatorias.", 
            SkillType.OFENSIVA, 
            4 // Cooldown de 4 turnos
        ));

        artillero.anadirHabilidadActiva(new Skill(
            "SKL_DEF_01", 
            "Cortina de Humo", 
            "Oculta tu tablero durante un turno.", 
            SkillType.DEFENSIVA, 
            5
        ));

        return artillero;
    }

    private GameCharacter crearComandante() {
        Skill pasiva = new Skill("PAS_02", "Refuerzo", "Repara 1 de vida cada 5 turnos", SkillType.PASIVA, 0);
        
        GameCharacter comandante = new GameCharacter("Comandante", "Líder táctico con gran defensa.", pasiva) {
            @Override
            public void aplicarEfectoPasivo(GameState estado, Player dueno) {
                // Lógica de reparación
            }
        };

        comandante.anadirHabilidadActiva(new Skill(
            "SKL_OFF_02", "Misil de Crucero", "Daña una zona de 3x3.", SkillType.OFENSIVA, 6
        ));

        comandante.anadirHabilidadActiva(new Skill(
            "SKL_DEF_02", "Escudo de Acero", "Bloquea el siguiente impacto.", SkillType.DEFENSIVA, 3
        ));

        return comandante;
    }
}
