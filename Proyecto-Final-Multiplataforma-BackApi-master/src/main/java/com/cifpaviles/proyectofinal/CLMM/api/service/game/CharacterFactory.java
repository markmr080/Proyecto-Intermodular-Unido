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
 * Consulta la base de datos para configurar la flota de cada personaje.
 */
@Service
public class CharacterFactory {

    private final PersonajeRepository personajeRepository;
    private final PersonajeFlotaRepository personajeFlotaRepository;

    public CharacterFactory(PersonajeRepository personajeRepository, PersonajeFlotaRepository personajeFlotaRepository) {
        this.personajeRepository = personajeRepository;
        this.personajeFlotaRepository = personajeFlotaRepository;
    }

    public GameCharacter crearPersonaje(String tipo) {
        GameCharacter baseCharacter;
        switch (tipo.toUpperCase()) {
            case "WULFRIK":
                baseCharacter = crearWulfrik();
                break;
            case "AISLINN":
                baseCharacter = crearAislinn();
                break;
            case "LOKHIR":
                baseCharacter = crearLokhir();
                break;
            case "ARANESSA":
                baseCharacter = crearAranessa();
                break;
            default:
                baseCharacter = crearWulfrik(); // Default
        }

        // Leer la flota desde la BD
        Optional<PersonajeEntity> optPersonaje = personajeRepository.findByNombre(baseCharacter.getNombre());
        if (optPersonaje.isPresent()) {
            List<PersonajeFlotaEntity> flota = personajeFlotaRepository.findByPersonaje(optPersonaje.get());
            for (PersonajeFlotaEntity pf : flota) {
                baseCharacter.anadirBarcoAFlota(pf.getBarcoTipo().getNombre(), pf.getCantidad());
            }
        } else {
            baseCharacter.anadirBarcoAFlota("Portaaviones", 1);
            baseCharacter.anadirBarcoAFlota("Acorazado", 2);
            baseCharacter.anadirBarcoAFlota("Crucero", 3);
            baseCharacter.anadirBarcoAFlota("Submarino", 4);
        }

        return baseCharacter;
    }

    private GameCharacter crearWulfrik() {
        Skill pasiva = new Skill("PAS_WUL", "Cazador de Naves", "Si aciertas a un barco enemigo, ganas un disparo extra.", SkillType.PASIVA, 0);
        GameCharacter c = new GameCharacter("Wulfrik", "Campeón de los dioses oscuros.", pasiva) {
            @Override
            public void aplicarEfectoPasivo(GameState estado, Player dueno) { }
        };
        c.anadirHabilidadActiva(new Skill("SKL_WUL_1", "Desafío del Errante", "Fuerza al rival a revelar la posición aleatoria de un barco si fallas.", SkillType.OFENSIVA, 4));
        c.anadirHabilidadActiva(new Skill("SKL_WUL_2", "Colmillo de los Mares", "Impacta un área en línea horizontal de 3 casillas.", SkillType.OFENSIVA, 5));
        c.anadirHabilidadActiva(new Skill("SKL_WUL_3", "Favor Ruinoso", "Escuda una casilla. Falla automática para el enemigo.", SkillType.DEFENSIVA, 4));
        return c;
    }

    private GameCharacter crearAislinn() {
        Skill pasiva = new Skill("PAS_AIS", "Señor del Mar Alto Elfo", "15% probabilidad de ignorar escudos/niebla.", SkillType.PASIVA, 0);
        GameCharacter c = new GameCharacter("Aislinn", "Comandante de la guardia del mar.", pasiva) {
            @Override
            public void aplicarEfectoPasivo(GameState estado, Player dueno) { }
        };
        c.anadirHabilidadActiva(new Skill("SKL_AIS_1", "Corte de Lothern", "Dos disparos independientes en dos casillas separadas.", SkillType.OFENSIVA, 4));
        c.anadirHabilidadActiva(new Skill("SKL_AIS_2", "Ira de Mathlann", "Golpea en forma de cruz (5 casillas).", SkillType.OFENSIVA, 6));
        c.anadirHabilidadActiva(new Skill("SKL_AIS_3", "Bruma Marina", "Oculta área 2x2. Si acierta, el golpe se anula.", SkillType.DEFENSIVA, 5));
        return c;
    }

    private GameCharacter crearLokhir() {
        Skill pasiva = new Skill("PAS_LOK", "Saqueador Especialista", "Al hundir un barco, revela una casilla del siguiente.", SkillType.PASIVA, 0);
        GameCharacter c = new GameCharacter("Lokhir", "Corsario de Karond Kar.", pasiva) {
            @Override
            public void aplicarEfectoPasivo(GameState estado, Player dueno) { }
        };
        c.anadirHabilidadActiva(new Skill("SKL_LOK_1", "Andanada Druchii", "Dispara a 3 casillas en diagonal.", SkillType.OFENSIVA, 4));
        c.anadirHabilidadActiva(new Skill("SKL_LOK_2", "Furia Corsaria", "Bengalas en área 3x3. Revela barcos sin causar daño.", SkillType.OFENSIVA, 5));
        c.anadirHabilidadActiva(new Skill("SKL_LOK_3", "Yelmo del Kraken", "Reubica uno de tus barcos enteros.", SkillType.DEFENSIVA, 6));
        return c;
    }

    private GameCharacter crearAranessa() {
        Skill pasiva = new Skill("PAS_ARA", "Casco Reforzado", "Tu barco más pequeño requiere dos impactos para hundirse.", SkillType.PASIVA, 0);
        GameCharacter c = new GameCharacter("Aranessa", "Reina Pirata de Sartosa.", pasiva) {
            @Override
            public void aplicarEfectoPasivo(GameState estado, Player dueno) { }
        };
        c.anadirHabilidadActiva(new Skill("SKL_ARA_1", "Pólvora Vampírica", "El fuego se propaga 2x2 si impacta un barco.", SkillType.OFENSIVA, 5));
        c.anadirHabilidadActiva(new Skill("SKL_ARA_2", "Disparo de Saloma", "Destruye forzosamente nieblas o escudos del tablero rival.", SkillType.OFENSIVA, 4));
        c.anadirHabilidadActiva(new Skill("SKL_ARA_3", "Hija de Stromfels", "Anula por un turno completo cualquier disparo.", SkillType.DEFENSIVA, 6));
        return c;
    }
}

