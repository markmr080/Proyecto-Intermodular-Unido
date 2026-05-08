# GameSocketController

Controlador WebSocket (Socket.IO) que gestiona **todos los eventos de la partida** en tiempo real.

## Archivo
- [GameSocketController.java](./GameSocketController.java)

## Descripción

Punto de entrada de todos los eventos WebSocket del juego. Recibe mensajes del cliente, delega en `GameEngine` y difunde el estado actualizado a todos los jugadores de la sala.

### Eventos manejados
| Evento | Método | Descripción |
|--------|--------|-------------|
| `join-room` | `onJoinRoom` | Crea/une a la sala. J1 crea el estado con su personaje; J2 reemplaza al dummy con su personaje elegido |
| `atacar` | `onAtacar` | Delega en `GameEngine.procesarDisparo()` |
| `usar-habilidad` | `onUsarHabilidad` | Delega en `GameEngine.usarHabilidad()` con coordenadas x,y |
| `colocar-barcos` | `onColocarBarcos` | Guarda el tablero del jugador y marca `listoParaCombate` |

### DTOs (clases internas)
| Clase | Campos | Descripción |
|-------|--------|-------------|
| `JoinMessage` | jugadorId, jugadorNombre, roomCode, **personajeId** | Datos de conexión a sala. `personajeId` determina el personaje asignado |
| `AtaqueMessage` | jugadorId, roomCode, x, y | Coordenadas del disparo |
| `HabilidadMessage` | jugadorId, roomCode, habilidadId, **x, y** | x,y son -1 para habilidades sin objetivo |
| `ColocarBarcosMessage` | jugadorId, roomCode, tablero | Tablero 10×10 con celdas BARCO/AGUA |

### Notas importantes
- El J2 en modo **test** nunca envía `join-room`; el dummy (`enemigo-dummy`) con WULFRIK permanece como J2.
- En partida **multijugador real**, J2 envía `join-room` con su `personajeId` y el controlador actualiza el personaje via `player.setPersonaje()`.

### Referencias
- `GameEngine.java` — motor de reglas que recibe las llamadas
- `RoomManager.java` — mantiene el mapa de `roomCode → GameEngine`
- `partida-activa` (frontend) — emite todos estos eventos vía `SocketService`
