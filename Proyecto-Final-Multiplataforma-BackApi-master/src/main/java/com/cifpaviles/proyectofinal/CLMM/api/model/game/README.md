# model/game — Modelos del Motor de Juego

Clases de dominio que representan el estado de una partida en memoria.

## Archivos

- [CellStatus.java](./CellStatus.java) — Enum de estados de celda del tablero
- [GameCharacter.java](./GameCharacter.java) — Personaje con pasiva, habilidades activas y flota
- [GameState.java](./GameState.java) — Estado global de la partida (turno, tableros, fase, ganador)
- [Player.java](./Player.java) — Jugador con tablero 10×10, escudos, flags de turno y estadísticas
- [Skill.java](./Skill.java) — Habilidad individual: ID, nombre, descripción, tipo y cooldown
- [SkillType.java](./SkillType.java) — Enum: `OFENSIVA`, `DEFENSIVA`, `PASIVA`

---

## Descripción

### CellStatus — Estado de celda

Cada celda del tablero 10×10 de cada jugador puede tener uno de estos estados:

| Valor | Descripción | Visible en tablero enemigo |
|-------|-------------|---------------------------|
| `AGUA` | Mar vacío sin atacar | Sí (agua sin marcar) |
| `BARCO` | Barco colocado, intacto | **No** (el enemigo no lo ve) |
| `REVELADA` | Barco descubierto por habilidad de visión — intacto pero conocido | **Sí** (👁️ ámbar) |
| `TOCADO` | Casilla de barco impactada | Sí (🔥) |
| `HUNDIDO` | Todas las celdas del barco fueron tocadas | Sí (🔥) |
| `AGUA_GOLPEADA` | Disparo fallido (agua) | Sí (💧) |

> `REVELADA` fue añadido para soportar las habilidades de visión:
> `SKL_WUL_1` (Desafío del Errante), `SKL_LOK_2` (Furia Corsaria) y la pasiva `PAS_LOK` (Saqueador Especialista).
> Una celda `REVELADA` se comporta como `BARCO` al recibir un impacto (causa daño normal).

### Player — Jugador

Campos destacados:

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `tablero` | `CellStatus[10][10]` | Estado de cada celda del tablero propio |
| `vidas` | `int` | Número de celdas `BARCO` originales — baja con cada impacto real |
| `escudoCasillas` | `Set<String>` | Coordenadas `"x,y"` con escudo activo |
| `escudoTotalActivo` | `boolean` | Invulnerabilidad total un turno (Aranessa SKL_ARA_3) |
| `turnoExtraWulfrik` | `boolean` | Permite segundo disparo en el mismo turno (PAS_WUL) |
| `habilidadUsadaEsteTurno` | `boolean` | Bloquea usar más de una habilidad por turno |
| `haAtacadoEsteTurno` | `boolean` | Bloquea atacar más de una vez (salvo turno extra) |

> **Escudos de casilla:** cuando un escudo absorbe un impacto, la celda **permanece como `BARCO`** — el barco no pierde vidas ni se altera visualmente. Solo el escudo desaparece. Esto es crítico para la coherencia de `vidas`.

### GameState — Estado global

Es el objeto que se serializa y envía al frontend vía Socket.IO en cada acción.
Contiene los dos objetos `Player` completos, incluyendo los tableros de ambos.

> El filtrado de `BARCO` → mostrar como `AGUA` en el tablero enemigo se hace **en el frontend** (`getClaseCasilla(estado, esMiTablero)`), no aquí.

### Skill — Habilidad

Cada habilidad tiene:
- `id` — identificador único (ej: `"SKL_WUL_1"`)
- `cooldownMax` — turnos de espera entre usos
- `cooldownActual` — turnos restantes hasta poder volver a usarla (se reduce con `reducirCooldown()`)
- `estaLista()` — devuelve `true` si `cooldownActual == 0`

---

## Referencias

- [GameEngine.java](../../service/game/GameEngine.java) — aplica la lógica usando estos modelos
- [CharacterFactory.java](../../service/game/CharacterFactory.java) — instancia los personajes con sus habilidades
- [GameSocketController.java](../../controller/GameSocketController.java) — serializa `GameState` y lo envía al frontend
- Frontend: [partida-activa.component.ts](../../../../../../../../front-conectado-main/src/app/partida-activa/partida-activa.component.ts)
