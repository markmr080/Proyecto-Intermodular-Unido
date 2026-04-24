package com.cifpaviles.proyectofinal.CLMM.api.service.game;

import com.cifpaviles.proyectofinal.CLMM.api.model.game.*;

/**
 * Clase encargada de instanciar personajes especÃ­ficos con sus habilidades.
 */
public class CharacterFactory {

    /**
     * Crea un personaje basado en su nombre o ID.
     */
    public static GameCharacter crearPersonaje(String tipo) {
        switch (tipo.toUpperCase()) {
            case "ARTILLERO":
                return crearArtillero();
            case "COMANDANTE":
                return crearComandante();
            default:
                return crearArtillero(); // Personaje por defecto
        }
    }

    private static GameCharacter crearArtillero() {
        // 1. Definir Habilidad Pasiva (Ej: "Recarga rÃ¡pida")
        Skill pasiva = new Skill("PAS_01", "PunterÃ­a", "Aumenta la probabilidad de crÃ­tico", SkillType.PASIVA, 0);

        // 2. Instanciar el personaje (usando una clase anÃ³nima o concreta)
        GameCharacter artillero = new GameCharacter("Artillero", "Experto en ataques mÃºltiples.", pasiva) {
            @Override
            public void aplicarEfectoPasivo(GameState estado, Player dueno) {
                // LÃ³gica de la pasiva: Se ejecuta automÃ¡ticamente.
            }
        };

        // 3. anadir Habilidad Ofensiva (Tu ejemplo: disparar 4 veces)
        artillero.anadirHabilidadActiva(new Skill(
            "SKL_OFF_01", 
            "RÃ¡faga", 
            "Dispara 4 veces en posiciones aleatorias.", 
            SkillType.OFENSIVA, 
            4 // Cooldown de 4 turnos
        ));

        // 4. anadir Habilidad Defensiva
        artillero.anadirHabilidadActiva(new Skill(
            "SKL_DEF_01", 
            "Cortina de Humo", 
            "Oculta tu tablero durante un turno.", 
            SkillType.DEFENSIVA, 
            5
        ));

        return artillero;
    }

    private static GameCharacter crearComandante() {
        Skill pasiva = new Skill("PAS_02", "Refuerzo", "Repara 1 de vida cada 5 turnos", SkillType.PASIVA, 0);
        
        GameCharacter comandante = new GameCharacter("Comandante", "LÃ­der tÃ¡ctico con gran defensa.", pasiva) {
            @Override
            public void aplicarEfectoPasivo(GameState estado, Player dueno) {
                // LÃ³gica de reparaciÃ³n
            }
        };

        comandante.anadirHabilidadActiva(new Skill(
            "SKL_OFF_02", "Misil de Crucero", "DaÃ±a una zona de 3x3.", SkillType.OFENSIVA, 6
        ));

        comandante.anadirHabilidadActiva(new Skill(
            "SKL_DEF_02", "Escudo de Acero", "Bloquea el siguiente impacto.", SkillType.DEFENSIVA, 3
        ));

        return comandante;
    }
}
