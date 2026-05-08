# socket.service.ts

Servicio Angular de **comunicación WebSocket** con el backend de juego (Socket.IO).

## Archivo
- [socket.service.ts](./socket.service.ts)

## Descripción

Centraliza toda la comunicación en tiempo real entre el frontend y el servidor de juego.
Expone métodos para emitir eventos al servidor y observables para escuchar respuestas.

### Eventos emitidos al servidor
| Método | Evento Socket | Descripción |
|--------|--------------|-------------|
| `joinRoom(id, nombre, roomCode, personajeId)` | `join-room` | Entra a la sala con el personaje elegido |
| `colocarBarcos(id, roomCode, tablero)` | `colocar-barcos` | Envía el tablero con barcos colocados |
| `atacar(id, roomCode, x, y)` | `atacar` | Dispara a la celda (x,y) del enemigo |
| `usarHabilidad(id, roomCode, habId, x, y)` | `usar-habilidad` | Usa una habilidad activa (x,y opcionales para habilidades de área) |
| `solicitarUnirse(roomCode, user)` | `solicitar-unirse` | Solicita al anfitrión unirse a su sala |
| `seleccionarPersonaje(roomCode, user, idx)` | `seleccionar-personaje` | Notifica al otro jugador de la selección |

### Observables de respuesta
| Observable | Evento escuchado | Descripción |
|------------|-----------------|-------------|
| `gameState$` | `game-state` | Estado completo del juego tras cada acción |
| `solicitudAceptada$` | `solicitud-aceptada` | El anfitrión aceptó la solicitud de unión |
| `solicitudRechazada$` | `solicitud-rechazada` | El anfitrión rechazó la solicitud |

### Notas
- `personajeId` en `joinRoom` es el tipo del personaje (ej: `'WULFRIK'`, `'AISLINN'`) leído de `localStorage`.
- `x` e `y` en `usarHabilidad` son `-1` para habilidades sin coordenada de objetivo.

### Referencias
- `GameSocketController.java` (backend) — procesa todos los eventos WebSocket
- `partida-activa` — consumidor principal del `gameState$`
- `lista-salas` — usa `solicitudAceptada$` y `solicitudRechazada$`
