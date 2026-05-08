# game — Motor de Juego (Backend)

Contiene el núcleo lógico del juego de batalla naval en el servidor.

## Archivos

### Modelo
- [GameState.java](../model/game/GameState.java) — Estado global de la partida (turno, tableros, fase, ganador)
- [Player.java](../model/game/Player.java) — Jugador con tablero, personaje, estadísticas y flags de turno (escudos, turnoExtraWulfrik)
- [GameCharacter.java](../model/game/GameCharacter.java) — Personaje base con pasiva, habilidades activas y flota
- [Skill.java](../model/game/Skill.java) — Habilidad individual (ID, nombre, tipo, cooldown)
- [CellStatus.java](../model/game/CellStatus.java) — Estados de celda: AGUA, BARCO, AGUA_GOLPEADA, TOCADO, HUNDIDO

### Servicio
- [GameEngine.java](./GameEngine.java) — Motor de reglas principal
- [CharacterFactory.java](./CharacterFactory.java) — Crea personajes con sus habilidades y carga flota desde BD

## Descripción

### GameEngine — Reglas del juego

Aplica toda la lógica de la partida:

#### `procesarDisparo(jugadorId, x, y)`
Procesa un disparo normal con integración de **pasivas**:
- **PAS_WUL (Wulfrik)**: al acertar un BARCO, concede un disparo extra en el mismo turno (`turnoExtraWulfrik = true`).
- **PAS_AIS (Aislinn)**: 20% de probabilidad de que el impacto entrante falle automáticamente.
- **PAS_LOK (Lokhir)**: al hundir un barco, revela en `mensajeEstado` la coordenada de un BARCO adyacente.
- Escudos de casilla (`escudoCasillas`): absorben el primer impacto en esa celda.
- Escudo total (`escudoTotalActivo`, Aranessa SKL_ARA_3): bloquea el turno completo del atacante.

#### `usarHabilidad(jugadorId, habilidadId, x, y)`
Ejecuta una habilidad activa:
- Las habilidades **OFENSIVA** terminan el turno (`cambiarTurno()` automático).
- Las habilidades **DEFENSIVA** no terminan el turno (son mejoras del mismo).
- `x, y = -1` para habilidades sin objetivo; coordenadas reales para habilidades de área.

#### Habilidades activas implementadas
| ID | Personaje | Tipo | Efecto |
|----|-----------|------|--------|
| SKL_WUL_1 | Wulfrik | Ofensiva | Revela posición de un BARCO enemigo |
| SKL_WUL_2 | Wulfrik | Ofensiva | Dispara 3 celdas en horizontal desde (x,y) |
| SKL_WUL_3 | Wulfrik | Defensiva | Escuda 1 celda BARCO propia aleatoria |
| SKL_AIS_1 | Aislinn | Ofensiva | 2 disparos aleatorios al enemigo |
| SKL_AIS_2 | Aislinn | Ofensiva | Cruz de 5 celdas centrada en (x,y) |
| SKL_AIS_3 | Aislinn | Defensiva | Escuda 4 celdas propias aleatorias |
| SKL_LOK_1 | Lokhir | Ofensiva | 3 disparos en diagonal desde (x,y) |
| SKL_LOK_2 | Lokhir | Ofensiva | Revela BARCOs en área 3×3 sin dañar |
| SKL_LOK_3 | Lokhir | Defensiva | Escuda 5 celdas propias |
| SKL_ARA_1 | Aranessa | Ofensiva | Disparo + propagación a 4 adyacentes si BARCO |
| SKL_ARA_2 | Aranessa | Ofensiva | Dispara a las 4 esquinas del tablero |
| SKL_ARA_3 | Aranessa | Defensiva | Escudo total para el siguiente turno |

### Player — Jugador
Añadidos en esta sesión:
- `escudoCasillas: Set<String>` — celdas con escudo activo (formato `"x,y"`)
- `escudoTotalActivo: boolean` — invulnerabilidad total un turno (Aranessa)
- `turnoExtraWulfrik: boolean` — permite un segundo disparo tras acierto
- `setPersonaje()` — para actualizar el personaje al unirse J2

### Referencias
- `GameSocketController.java` — llama a `procesarDisparo()` y `usarHabilidad()` desde los eventos WebSocket
- `GameState.cambiarTurno()` — resetea flags de ambos jugadores (incluido `turnoExtraWulfrik`)
- `DataInitializer.java` — inicializa las flotas únicas de cada personaje en la BD
