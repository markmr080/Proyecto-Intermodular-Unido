package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeFlotaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.game.*;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeFlotaRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public CharacterFactory(PersonajeRepository personajeRepository,
            PersonajeFlotaRepository personajeFlotaRepository) {
        this.personajeRepository = personajeRepository;
        this.personajeFlotaRepository = personajeFlotaRepository;
    }

    @Transactional
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
            case "IKIT":
                baseCharacter = crearIkit();
                break;
            case "GELT":
                baseCharacter = crearGelt();
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
            baseCharacter.anadirBarcoAFlota("Destructor", 4);
        }

        return baseCharacter;
    }

    private GameCharacter crearWulfrik() {
        Skill pasiva = new Skill("PAS_WUL", "Cazador de Naves",
                "Si aciertas a un barco enemigo, ganas un disparo extra.", SkillType.PASIVA, 0, "/imagenes/wulfrik_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Wulfrik", "Campeón de los dioses oscuros.", "/imagenes/wulfrik.jpg", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_WUL_1", "Desafío del Errante",
                "Lanza un disparo; si fallas, se revela la posición aleatoria de un barco enemigo.", SkillType.OFENSIVA, 4, "/imagenes/wulfrik_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_WUL_2", "Colmillo de los Mares",
                "Impacta un área en línea horizontal de 3 casillas.", SkillType.OFENSIVA, 5, "/imagenes/wulfrik_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_WUL_3", "Favor Ruinoso",
                "Escuda una casilla. Falla automática para el enemigo.", SkillType.DEFENSIVA, 4, "/imagenes/wulfrik_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearAislinn() {
        Skill pasiva = new Skill("PAS_AIS", "Señor del Mar Alto Elfo", "20% probabilidad de ignorar escudos y protecciones.",
                SkillType.PASIVA, 0, "/imagenes/aislinn_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Aislinn", "Comandante de la guardia del mar.", "/imagenes/aislinn.jpg", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_AIS_1", "Corte de Lothern",
                "Dos disparos independientes en dos casillas separadas.", SkillType.OFENSIVA, 4, "/imagenes/aislinn_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_AIS_2", "Ira de Mathlann", "Golpea en forma de cruz (5 casillas).",
                SkillType.OFENSIVA, 6, "/imagenes/aislinn_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_AIS_3", "Bruma Marina",
                "Despliega una niebla en un área 2x2 de tu tablero que protege tus barcos.", SkillType.DEFENSIVA, 5, "/imagenes/aislinn_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearLokhir() {
        Skill pasiva = new Skill("PAS_LOK", "Saqueador Especialista",
                "Al hundir un barco, revela una casilla del siguiente.", SkillType.PASIVA, 0, "/imagenes/lokhir_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Lokhir", "Corsario de Karond Kar.", "/imagenes/lokhir.webp", pasiva);
        c.anadirHabilidadActiva(
                new Skill("SKL_LOK_1", "Andanada Druchii", "Dispara a 3 casillas en diagonal.", SkillType.OFENSIVA, 4, "/imagenes/lokhir_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_LOK_2", "Furia Corsaria",
                "Bengalas en área 3x3. Revela barcos sin causar daño.", SkillType.OFENSIVA, 5, "/imagenes/lokhir_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_LOK_3", "Yelmo del Kraken", "Protege todas las casillas de tu barco más grande (Arca Negra). El escudo desaparece por completo al recibir el primer impacto.",
                SkillType.DEFENSIVA, 6, "/imagenes/lokhir_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearAranessa() {
        Skill pasiva = new Skill("PAS_ARA", "Tripulación de los Muertos",
                "Los marineros no-muertos no temen a la muerte. 20% de probabilidad de ignorar el daño recibido.", SkillType.PASIVA, 0, "/imagenes/aranessa_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Aranessa", "Reina Pirata de Sartosa.", "/imagenes/Aranessa (1).jpg", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_ARA_1", "Pólvora Vampírica",
                "El fuego se propaga en cruz (4 casillas adyacentes) si impacta un barco.", SkillType.OFENSIVA, 5, "/imagenes/aranessa_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_ARA_2", "Disparo de Saloma",
                "Elimina TODOS los escudos del rival y dispara en área 2x2.", SkillType.OFENSIVA, 4, "/imagenes/aranessa_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_ARA_3", "Hija de Stromfels",
                "Escudo total: el próximo disparo enemigo sobre cualquier casilla fallará.", SkillType.DEFENSIVA, 6, "/imagenes/aranessa_habilidades/defensiva.png"));
        return c;
    }
    private GameCharacter crearIkit() {
        Skill pasiva = new Skill("PAS_IKT", "Ingeniero Brujo de Skryre",
                "20% de probabilidad de que una habilidad ofensiva no consuma enfriamiento.", SkillType.PASIVA, 0, "/imagenes/ikitclaw_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Ikit Claw", "El Gran Arquitecto de la Destrucción.", "/imagenes/ikitclaw.jpg", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_IKT_1", "Rayo de Piedra Bruja",
                "Lanza un rayo potente que impacta una casilla y revela las adyacentes en cruz.", SkillType.OFENSIVA, 4, "/imagenes/ikitclaw_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_IKT_2", "Cohete de Muerte",
                "Impacta un área masiva de 3x3 casillas.", SkillType.OFENSIVA, 8, "/imagenes/ikitclaw_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_IKT_3", "Escudo de Piedra Bruja",
                "Protege un área aleatoria de 2x2 casillas contra el próximo disparo.", SkillType.DEFENSIVA, 6, "/imagenes/ikitclaw_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearGelt() {
        Skill pasiva = new Skill("PAS_GEL", "Metalurgia Dorada",
                "Al impactar un barco, reduce el enfriamiento de una habilidad activa aleatoria en 1 turno.", SkillType.PASIVA, 0, "/imagenes/gelt_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Balthasar Gelt", "Patriarca Supremo de los Colegios de Magia.", "/imagenes/gelt.png", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_GEL_1", "Transmutación de Plomo",
                "Convierte una zona 2x2 en oro: revela barcos y causa daño.", SkillType.OFENSIVA, 5, "/imagenes/gelt_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_GEL_2", "Lluvia de Metal",
                "Invoca una lluvia de proyectiles que impacta en 3 casillas aleatorias del tablero enemigo.", SkillType.OFENSIVA, 4, "/imagenes/gelt_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_GEL_3", "Cuerpo de Hierro",
                "Protege todas las casillas de tu barco más grande con escudos mágicos.", SkillType.DEFENSIVA, 6, "/imagenes/gelt_habilidades/defensiva.png"));
        return c;
    }

    /** Habilidad especial de Lokhir cuando pierde el Arca Negra. */
    public static Skill crearVenganzaLokhir() {
        return new Skill("SKL_LOK_4", "Venganza de Karond Kar",
                "Bombardeo masivo: realiza 5 disparos aleatorios sobre el tablero enemigo.",
                SkillType.OFENSIVA, 3, "/imagenes/lokhir_habilidades/ofensiva3.png");
    }
}
