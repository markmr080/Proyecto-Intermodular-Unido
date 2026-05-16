package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.game;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.game.*;
import org.springframework.stereotype.Service;

@Service
public class CharacterFactory {

    private final com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient backendClient;

    public CharacterFactory(com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    public GameCharacter crearPersonaje(String tipo) {
        GameCharacter baseCharacter;
        String tipoUpper = tipo != null ? tipo.toUpperCase() : "WULFRIK";
        switch (tipoUpper) {
            case "WULFRIK":
                baseCharacter = crearWulfrik();
                baseCharacter.anadirBarcoAFlota("Portaaviones", 1);
                baseCharacter.anadirBarcoAFlota("Acorazado",    1);
                baseCharacter.anadirBarcoAFlota("Crucero",      2);
                baseCharacter.anadirBarcoAFlota("Destructor",   1);
                break;
            case "AISLINN":
                baseCharacter = crearAislinn();
                baseCharacter.anadirBarcoAFlota("Portaaviones", 1);
                baseCharacter.anadirBarcoAFlota("Acorazado",    1);
                baseCharacter.anadirBarcoAFlota("Crucero",      1);
                baseCharacter.anadirBarcoAFlota("Destructor",   2);
                break;
            case "LOKHIR":
                baseCharacter = crearLokhir();
                baseCharacter.anadirBarcoAFlota("Portaaviones", 1);
                baseCharacter.anadirBarcoAFlota("Crucero",      2);
                baseCharacter.anadirBarcoAFlota("Destructor",   2);
                break;
            case "ARANESSA":
                baseCharacter = crearAranessa();
                baseCharacter.anadirBarcoAFlota("Acorazado", 2);
                baseCharacter.anadirBarcoAFlota("Crucero",   2);
                baseCharacter.anadirBarcoAFlota("Destructor",1);
                break;
            case "IKIT":
            case "IKIT CLAW":
                baseCharacter = crearIkit();
                baseCharacter.anadirBarcoAFlota("Acorazado", 1);
                baseCharacter.anadirBarcoAFlota("Crucero",   2);
                baseCharacter.anadirBarcoAFlota("Destructor",2);
                break;
            case "GELT":
            case "BALTHASAR GELT":
                baseCharacter = crearGelt();
                baseCharacter.anadirBarcoAFlota("Portaaviones", 1);
                baseCharacter.anadirBarcoAFlota("Acorazado",    1);
                baseCharacter.anadirBarcoAFlota("Crucero",      2);
                baseCharacter.anadirBarcoAFlota("Destructor",   1);
                break;
            default:
                baseCharacter = crearWulfrik();
                baseCharacter.anadirBarcoAFlota("Portaaviones", 1);
                baseCharacter.anadirBarcoAFlota("Acorazado",    1);
                baseCharacter.anadirBarcoAFlota("Crucero",      2);
                baseCharacter.anadirBarcoAFlota("Destructor",   1);
        }

        // Intentar sobrescribir con los datos reales del backend (no bloquea si falla)
        try {
            java.util.List<com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.PersonajeDTO> personajes = backendClient.getPersonajes();
            final GameCharacter finalChar = baseCharacter;
            personajes.stream()
                .filter(p -> p.getNombre().equalsIgnoreCase(finalChar.getNombre()))
                .findFirst()
                .ifPresent(p -> {
                    if (p.getFlota() != null && !p.getFlota().isEmpty()) {
                        // Limpiar flota por defecto y usar la del backend
                        finalChar.getFlotaPermitida().clear();
                        p.getFlota().forEach(f -> finalChar.anadirBarcoAFlota(f.getTipoBarco(), f.getCantidad()));
                        System.out.println("Flota cargada desde BD para: " + finalChar.getNombre() + " -> " + finalChar.getFlotaPermitida());
                    }
                });
        } catch (Exception e) {
            System.err.println("Backend no disponible para personajes, usando flota por defecto: " + e.getMessage());
        }

        System.out.println("Personaje creado: " + baseCharacter.getNombre() + " | Flota: " + baseCharacter.getFlotaPermitida() + " | Vidas: " + baseCharacter.getTotalHealth());
        return baseCharacter;
    }

    private GameCharacter crearWulfrik() {
        Skill pasiva = new Skill("PAS_WUL", "Cazador de Naves",
                "Si aciertas a un barco enemigo, ganas un disparo extra.", SkillType.PASIVA, 0, "/imagenes/wulfrik_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Wulfrik", "Campeón de los dioses oscuros.", "/imagenes/wulfrik.jpg", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_WUL_1", "Desafío del Errante",
                "Lanza un disparo; si fallas, se revela la posición aleatoria de un barco enemigo.", SkillType.ACTIVA_OFENSIVA, 4, "/imagenes/wulfrik_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_WUL_2", "Colmillo de los Mares",
                "Impacta en la casilla objetivo y en las de su izquierda y derecha (3 en horizontal).", SkillType.ACTIVA_OFENSIVA, 5, "/imagenes/wulfrik_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_WUL_3", "Favor Ruinoso",
                "Escuda una casilla. Falla automática para el enemigo.", SkillType.ACTIVA_DEFENSIVA, 4, "/imagenes/wulfrik_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearAislinn() {
        Skill pasiva = new Skill("PAS_AIS", "Señor del Mar Alto Elfo", "20% probabilidad de ignorar escudos y protecciones.",
                SkillType.PASIVA, 0, "/imagenes/aislinn_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Aislinn", "Comandante de la guardia del mar.", "/imagenes/aislinn.jpg", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_AIS_1", "Corte de Lothern",
                "Dos disparos independientes en dos casillas separadas.", SkillType.ACTIVA_OFENSIVA, 4, "/imagenes/aislinn_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_AIS_2", "Ira de Mathlann", "Golpea en forma de cruz (5 casillas).",
                SkillType.ACTIVA_OFENSIVA, 6, "/imagenes/aislinn_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_AIS_3", "Bruma Marina",
                "Despliega una niebla en un área 2x2 que protege tus barcos.", SkillType.ACTIVA_DEFENSIVA, 5, "/imagenes/aislinn_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearLokhir() {
        Skill pasiva = new Skill("PAS_LOK", "Saqueador Especialista",
                "Al hundir un barco, revela una casilla del siguiente.", SkillType.PASIVA, 0, "/imagenes/lokhir_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Lokhir", "Corsario de Karond Kar.", "/imagenes/lokhir.webp", pasiva);
        c.anadirHabilidadActiva(
                new Skill("SKL_LOK_1", "Andanada Druchii", "Dispara en forma de X: impacta la casilla central y las 4 diagonales.", SkillType.ACTIVA_OFENSIVA, 4, "/imagenes/lokhir_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_LOK_2", "Furia Corsaria",
                "Bengalas en área 3x3. Revela barcos sin causar daño.", SkillType.ACTIVA_OFENSIVA, 5, "/imagenes/lokhir_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_LOK_3", "Yelmo del Kraken", "Protege todas las casillas de tu barco más grande (Arca Negra). El escudo desaparece por completo al recibir el primer impacto.",
                SkillType.ACTIVA_DEFENSIVA, 6, "/imagenes/lokhir_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearAranessa() {
        Skill pasiva = new Skill("PAS_ARA", "Tripulación de los Muertos",
                "Los marineros no-muertos no temen a la muerte. 20% de probabilidad de ignorar el daño recibido.", SkillType.PASIVA, 0, "/imagenes/aranessa_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Aranessa", "Reina Pirata de Sartosa.", "/imagenes/Aranessa (1).jpg", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_ARA_1", "Pólvora Vampírica",
                "El fuego se propaga en cruz (4 casillas adyacentes) si impacta un barco.", SkillType.ACTIVA_OFENSIVA, 5, "/imagenes/aranessa_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_ARA_2", "Disparo de Saloma",
                "Ignora y destruye todos los escudos del rival (disparo en área 2x2).", SkillType.ACTIVA_OFENSIVA, 4, "/imagenes/aranessa_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_ARA_3", "Hija de Stromfels",
                "Escudo total: el próximo disparo enemigo sobre cualquier casilla fallará.", SkillType.ACTIVA_DEFENSIVA, 6, "/imagenes/aranessa_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearIkit() {
        Skill pasiva = new Skill("PAS_IKT", "Ingeniero Brujo de Skryre",
                "20% de probabilidad de que una habilidad ofensiva no consuma enfriamiento.", SkillType.PASIVA, 0, "/imagenes/ikitclaw_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Ikit Claw", "El Gran Arquitecto de la Destrucción.", "/imagenes/ikitclaw.jpg", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_IKT_1", "Rayo de Piedra Bruja",
                "Lanza un rayo potente que impacta una casilla y revela las adyacentes en cruz.", SkillType.ACTIVA_OFENSIVA, 4, "/imagenes/ikitclaw_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_IKT_2", "Cohete de Muerte",
                "Impacta un área masiva de 3x3 casillas.", SkillType.ACTIVA_OFENSIVA, 8, "/imagenes/ikitclaw_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_IKT_3", "Escudo de Piedra Bruja",
                "Protege un área aleatoria de 2x2 casillas contra el próximo disparo.", SkillType.ACTIVA_DEFENSIVA, 6, "/imagenes/ikitclaw_habilidades/defensiva.png"));
        return c;
    }

    private GameCharacter crearGelt() {
        Skill pasiva = new Skill("PAS_GEL", "Metalurgia Dorada",
                "Al impactar un barco, devuelve enfriamiento de una habilidad activa aleatoria en 1 turno.", SkillType.PASIVA, 0, "/imagenes/gelt_habilidades/pasiva.png");
        GameCharacter c = new GameCharacter("Balthasar Gelt", "Patriarca Supremo de los Colegios de Magia.", "/imagenes/gelt.png", pasiva);
        c.anadirHabilidadActiva(new Skill("SKL_GEL_1", "Transmutación de Plomo",
                "Convierte una zona 2x2 en oro: revela barcos y causa daño.", SkillType.ACTIVA_OFENSIVA, 5, "/imagenes/gelt_habilidades/ofensiva1.png"));
        c.anadirHabilidadActiva(new Skill("SKL_GEL_2", "Lluvia de Metal",
                "Invoca una lluvia de proyectiles que impacta en 3 casillas aleatorias.", SkillType.ACTIVA_OFENSIVA, 4, "/imagenes/gelt_habilidades/ofensiva2.png"));
        c.anadirHabilidadActiva(new Skill("SKL_GEL_3", "Cuerpo de Hierro",
                "Protege todas las casillas de tu barco más grande con escudos mágicos.", SkillType.ACTIVA_DEFENSIVA, 6, "/imagenes/gelt_habilidades/defensiva.png"));
        return c;
    }

    public static Skill crearVenganzaLokhir() {
        return new Skill("SKL_LOK_4", "Venganza de Karond Kar", "Desata un bombardeo aleatorio de 5 disparos tras perder el Arca Negra.", SkillType.ACTIVA_OFENSIVA, 5, "/imagenes/lokhir_habilidades/ofensiva2.png");
    }
}
