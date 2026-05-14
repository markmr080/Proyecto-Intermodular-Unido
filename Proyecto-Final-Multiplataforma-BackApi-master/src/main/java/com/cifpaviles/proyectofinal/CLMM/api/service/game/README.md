# game — Motor de Juego (Backend)

Contiene el núcleo lógico del juego de batalla naval en el servidor.

## Archivos

### Modelo
- [GameState.java](../model/game/GameState.java) — Estado global de la partida (turno, tableros, fase, ganador)
- [Player.java](../model/game/Player.java) — Jugador con tablero, personaje, estadísticas y flags de turno (escudos, turnoExtraWulfrik)
- [GameCharacter.java](../model/game/GameCharacter.java) — Personaje base con pasiva, habilidades activas y flota
- [Skill.java](../model/game/Skill.java) — Habilidad individual (ID, nombre, tipo, cooldown)
- [CellStatus.java](../model/game/CellStatus.java) — Estados de celda: AGUA, BARCO, AGUA_GOLPEADA, TOCADO, HUNDIDO, **REVELADA**

### Servicio
- [GameEngine.java](./GameEngine.java) — Motor de reglas principal
- [CharacterFactory.java](./CharacterFactory.java) — Crea personajes con sus habilidades y carga flota desde BD

---

## Descripción

### GameEngine — Reglas del juego

Aplica toda la lógica de la partida.

#### `procesarDisparo(jugadorId, x, y)`
Procesa un disparo normal con integración de **pasivas**:
- **PAS_WUL (Wulfrik)**: al acertar un BARCO, concede un disparo extra en el mismo turno (`turnoExtraWulfrik = true`).
- **PAS_AIS (Aislinn)**: 20% de probabilidad de que el impacto entrante falle automáticamente.
- **PAS_LOK (Lokhir)**: al hundir un barco, revela una celda `BARCO` del resto de la flota enemiga (o adyacente si es posible).
- **Escudos de casilla** (`escudoCasillas`): absorben el primer impacto en esa celda. La celda **permanece como `BARCO`**. En Lokhir, los escudos del Arca Negra son frágiles y caen todos al primer impacto.
- **Escudo total** (`escudoTotalActivo`, Aranessa SKL_ARA_3): bloquea el turno completo del atacante sin modificar el tablero.

> ⚠️ **Comportamiento correcto del escudo de casilla:** la celda NO se convierte en `AGUA_GOLPEADA`. El barco sigue en pie; solo el escudo desaparece. Esto es crítico para la integridad de `vidas` — si la celda se convirtiera a `AGUA_GOLPEADA`, el barco quedaría partido sin que `vidas` bajara, bloqueando la partida.

#### `usarHabilidad(jugadorId, habilidadId, x, y)`
Ejecuta una habilidad activa:
- Las habilidades **OFENSIVA** terminan el turno (`cambiarTurno()` automático).
- Las habilidades **DEFENSIVA** no terminan el turno (son mejoras del mismo).
- `x, y = -1` para habilidades sin objetivo; coordenadas reales para habilidades de área.

---

### Habilidades activas implementadas

| ID | Personaje | Tipo | Efecto |
|----|-----------|------|--------|
| SKL_WUL_1 | Wulfrik | Ofensiva | Marca un `BARCO` enemigo aleatorio como `REVELADA` (visible en tablero) |
| SKL_WUL_2 | Wulfrik | Ofensiva | Dispara 3 celdas en horizontal desde (x,y) |
| SKL_WUL_3 | Wulfrik | Defensiva | Escuda 1 celda `BARCO` propia aleatoria |
| SKL_AIS_1 | Aislinn | Ofensiva | 2 disparos aleatorios al enemigo |
| SKL_AIS_2 | Aislinn | Ofensiva | Cruz de 5 celdas centrada en (x,y) |
| SKL_AIS_3 | Aislinn | Defensiva | Escuda área 2x2 en el tablero propio (manual) |
| SKL_LOK_1 | Lokhir | Ofensiva | 3 disparos en diagonal desde (x,y) |
| SKL_LOK_2 | Lokhir | Ofensiva | Marca como `REVELADA` los `BARCO` en área 3×3 sin dañarlos |
| SKL_LOK_3 | Lokhir | Defensiva | Escuda el Arca Negra (barco más grande); el escudo cae al primer impacto |
| SKL_LOK_4 | Lokhir | Ofensiva | **Venganza**: Lanza 5 disparos aleatorios (sustituye a SKL_LOK_3 al caer el Arca) |
| SKL_ARA_1 | Aranessa | Ofensiva | Disparo + propagación a 4 adyacentes si impacta `BARCO` |
| SKL_ARA_2 | Aranessa | Ofensiva | Dispara a las 4 esquinas del tablero |
| SKL_ARA_3 | Aranessa | Defensiva | Escudo total para el siguiente turno |
| SKL_IKT_1 | Ikit Claw | Ofensiva | Rayo que impacta y revela adyacentes en cruz |
| SKL_IKT_2 | Ikit Claw | Ofensiva | Impacta área masiva 3x3 |
| SKL_IKT_3 | Ikit Claw | Defensiva | Escuda área aleatoria 2x2 |
| SKL_GEL_1 | B. Gelt | Ofensiva | Transmutación: área 2x2 (daño + reveal) |
| SKL_GEL_2 | B. Gelt | Ofensiva | Lluvia de metal: 3 disparos aleatorios |
| SKL_GEL_3 | B. Gelt | Defensiva | Escuda el barco más grande del jugador |

---

### Habilidades de visión — Estado `REVELADA`

Las habilidades **SKL_WUL_1**, **SKL_LOK_2** y la pasiva **PAS_LOK** no solo informan
por texto — marcan la celda del tablero enemigo como `CellStatus.REVELADA`.

El frontend renderiza `REVELADA` con el icono 👁️ y fondo ámbar pulsante (`casilla-revelada`).
Atacar una celda `REVELADA` la trata como `BARCO` (inflige daño normal).

---

### SKL_LOK_3 — Yelmo del Kraken (Escudo del Arca Negra)

Mecánica de protección del Arca:

1. `encontrarBarcosCompletos(tablero)` — Identifica todos los barcos del jugador.
2. Identifica el barco más grande (tamaño 5, el Arca Negra).
3. Aplica un escudo a cada una de sus casillas activas.
4. Si cualquier casilla del Arca Negra recibe un impacto (normal o habilidad), el sistema detecta la pertenencia al barco insignia y **elimina todos los escudos restantes** de dicho barco.

> El escudo protege contra el daño del primer impacto, pero deja el resto del barco vulnerable para ataques subsiguientes en el mismo turno o turnos futuros.

---

### Player — Jugador
Campos relevantes:
- `escudoCasillas: Set<String>` — celdas con escudo activo (formato `"x,y"`)
- `escudoTotalActivo: boolean` — invulnerabilidad total un turno (Aranessa)
- `turnoExtraWulfrik: boolean` — permite un segundo disparo tras acierto
- `setPersonaje()` — para actualizar el personaje al unirse J2

---

### Referencias
- [GameSocketController.java](../../controller/GameSocketController.java) — llama a `procesarDisparo()` y `usarHabilidad()` desde los eventos WebSocket
- `GameState.cambiarTurno()` — resetea flags de ambos jugadores (incluido `turnoExtraWulfrik`)
- [CellStatus.java](../model/game/CellStatus.java) — enum con todos los estados de celda
