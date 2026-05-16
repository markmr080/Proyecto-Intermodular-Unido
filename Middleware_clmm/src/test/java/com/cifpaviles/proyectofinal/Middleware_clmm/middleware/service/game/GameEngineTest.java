package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.game;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    private GameEngine gameEngine;
    private GameState state;
    private Player p1;
    private Player p2;

    @BeforeEach
    void setUp() {
        // Crear personajes básicos
        Skill pasiva = new Skill("PAS_TEST", "Test Pasiva", "Descripción", SkillType.PASIVA, 0, "");
        GameCharacter c1 = new GameCharacter("Wulfrik", "Desc", "img", pasiva);
        GameCharacter c2 = new GameCharacter("Aislinn", "Desc", "img", pasiva);

        p1 = new Player("1", "Jugador 1", c1);
        p2 = new Player("2", "Jugador 2", c2);

        // Darle vidas manualmente para que no empiecen muertos (getTotalHealth daría 0)
        p1.setVidas(10);
        p2.setVidas(10);

        state = new GameState(p1, p2);
        state.setFase("COMBATE");
        state.setTurnoActualId("1");
        
        gameEngine = new GameEngine(state);
    }

    @Test
    void testProcesarDisparo_Impacto() {
        // Colocar dos casillas de barco para que no se hunda al primer disparo
        p2.getTablero()[5][5] = CellStatus.BARCO;
        p2.getTablero()[5][6] = CellStatus.BARCO;
        int vidasIniciales = p2.getVidas();

        gameEngine.procesarDisparo("1", 5, 5);

        assertEquals(CellStatus.TOCADO, p2.getTablero()[5][5]);
        assertEquals(vidasIniciales - 1, p2.getVidas());
        assertEquals(1, p1.getHitsAcertados());
        assertEquals("2", state.getTurnoActualId()); // Cambio de turno
    }

    @Test
    void testProcesarDisparo_Agua() {
        p2.getTablero()[5][5] = CellStatus.AGUA;

        gameEngine.procesarDisparo("1", 5, 5);

        assertEquals(CellStatus.AGUA_GOLPEADA, p2.getTablero()[5][5]);
        assertEquals(1, p1.getHitsFallados());
        assertEquals("2", state.getTurnoActualId());
    }

    @Test
    void testProcesarDisparo_FueraDeTurno() {
        gameEngine.procesarDisparo("2", 5, 5); // Le toca al 1

        assertEquals("1", state.getTurnoActualId()); // Sigue siendo turno del 1
        assertEquals(0, p1.getHitsAcertados());
        assertEquals(0, p1.getHitsFallados());
    }

    @Test
    void testProcesarDisparo_Repetido() {
        p2.getTablero()[5][5] = CellStatus.TOCADO;
        
        gameEngine.procesarDisparo("1", 5, 5);

        // No debe cambiar el turno porque el disparo fue ignorado
        assertEquals("1", state.getTurnoActualId());
        assertEquals(0, p1.getHitsAcertados());
    }

    @Test
    void testVerificarVictoria() {
        // Forzamos a que tengan vidas antes del disparo final
        p1.setVidas(5);
        p2.setVidas(1);
        p2.getTablero()[0][0] = CellStatus.BARCO;
        
        gameEngine.procesarDisparo("1", 0, 0);
        
        assertFalse(state.isJuegoActivo());
        assertEquals("1", state.getGanadorId());
    }
}
