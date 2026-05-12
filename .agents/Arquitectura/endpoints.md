# ENDPOINTS Y EVENTOS WEB SOCKET

> **Actualizado:** 2026-05-12

Este documento detalla todos los endpoints REST y los eventos de WebSocket que existen en el sistema, describiendo su funcionamiento y parámetros esperados.

## 1. Endpoints de Autenticación (`/api/auth`)

Estos endpoints pertenecen al Middleware y gestionan el ciclo de vida del acceso. Todos (excepto `/login`) requieren que la cabecera contenga un token de acceso y un fingerprint `X-Fingerprint`.

| Método | Endpoint | Descripción | Requiere Auth |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/auth/login` | Recibe las credenciales fijas del middleware (`middleware_admin`) y un fingerprint generado por el cliente. Devuelve el JWT temporal base. | No |
| **POST** | `/api/auth/validate-user` | Recibe credenciales reales del usuario (ej: `username` y `password`). Se valida internamente contra BBDD y si es correcto, inicia la sesión lógica para el cliente devolviendo la información básica del jugador. | Sí (Token temporal) |
| **POST** | `/api/auth/register` | Registra un nuevo usuario en la Base de Datos. Recibe `username`, `email` y `password`. | Sí (Token temporal) |
| **POST** | `/api/auth/forgot-password` | Envía un correo con un link único y un JWT embebido para cambiar la contraseña. | Sí (Token temporal) |
| **POST** | `/api/auth/reset-password` | Recibe el token del correo electrónico y la nueva contraseña. Modifica el hash en la BBDD. | Sí (Token temporal) |
| **POST** | `/api/auth/update-password` | Para un usuario ya logueado que quiere cambiar su contraseña. Requiere la password nueva. | Sí |
| **POST** | `/api/auth/update-nickname` | Cambia el apodo del jugador. | Sí |
| **POST** | `/api/auth/update-profile-picture` | Actualiza la url o hash (Dicebear) del avatar de jugador. | Sí |

---

## 2. Endpoints de Estadísticas (`/api/estadisticas`)

| Método | Endpoint | Descripción | Requiere Auth |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/estadisticas/jugador/{username}` | Devuelve los stats del jugador guardados en la BD (MySQL y MongoDB) como el ratio de precisión, partidas ganadas, barcos hundidos. | Sí |

---

## 3. Endpoints del Lobby (`/api/lobby`)

Punto de entrada de las peticiones REST asociadas a las salas previas (Lobby) manejadas en memoria en el Middleware.

| Método | Endpoint | Descripción | Requiere Auth |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/lobby` | Devuelve el listado de las salas actuales que están en estado `ESPERANDO`. | Sí |
| **POST** | `/api/lobby` | Crea una nueva sala in-memory para el jugador que la invoca. Devuelve el `LobbyRoom` creado que contiene un código alfanumérico único. | Sí |
| **DELETE** | `/api/lobby/{codigoSala}`| Borra una sala del `LobbyManager`. | Sí |

---

## 4. Eventos de WebSockets (Lobby y Combate)

Se conectan a través de Socket.IO en un solo puerto unificado en el servidor (ej: 8081). Se agrupan en dos grandes fases lógicas: el Pre-Juego (Lobby) y el Juego (Tablero).

### 4.1. Eventos Fase de Lobby (Sala de espera)

| Evento (Emit) | Datos enviados | Respuesta (On) esperada | Descripción |
| :--- | :--- | :--- | :--- |
| `registrar-usuario` | `username` | Ninguna explícita | Asocia el ID de conexión de Socket.IO con el nombre de usuario para notificaciones directas. |
| `solicitar-unirse` | `{codigoSala, requesterId, requesterName, requesterAvatar}` | `nueva-solicitud` al Anfitrión | Un jugador pide entrar a la sala del creador. |
| `aceptar-solicitud` | `{codigoSala, requesterId...}` | `solicitud-aceptada` (con código de sala) al Requester y `jugador-unido` global | El creador de la sala acepta al jugador. Ambos clientes navegan al seleccionador de personajes. |
| `rechazar-solicitud` | `requesterId` | `solicitud-rechazada` al Requester | Denegación explícita de entrada. |
| `seleccionar-personaje`| `{codigoSala, userId, personajeId}` | `personaje-seleccionado` (Broadcast en sala) | Avisa al rival de qué personaje (capitán) ha bloqueado el jugador actual. |
| `comenzar-juego` | `codigoSala` | `juego-comenzado` | Cuando ambos están listos, el Host dispara este evento para forzar la navegación al Tablero en el frontend. |

### 4.2. Eventos Fase de Combate (Tablero)

Una vez en `partida-activa`, la comunicación fluye constantemente entre los clientes y el `GameEngine`.

| Evento (Emit) | Datos enviados | Respuesta (On) esperada | Descripción |
| :--- | :--- | :--- | :--- |
| `join-room` | `{jugadorId, jugadorNombre, roomCode}` | `gameState` | Mete al usuario a la sala técnica del combate en la memoria del GameEngine. Al unirse los dos, arranca la fase de COLOCACION. |
| `colocar-barcos` | `{jugadorId, roomCode, tablero[10][10]}`| `gameState` | Envía la matriz con la posición de los 5 barcos. Cuando ambos envían, arranca la fase COMBATE. |
| `atacar` | `{jugadorId, roomCode, x, y}` | `gameState` | Realiza un ataque básico en la coordenada enviada. El Engine procesa las reglas (hits, fallos) y actualiza el estado. |
| `usar-habilidad` | `{jugadorId, roomCode, habilidadId, x, y}` | `gameState` | Invoca una mecánica especial del personaje. `x,y` son las coordenadas de celda objetivo para habilidades de área (`-1,-1` para las que no necesitan objetivo). |
| `rendirse` | `{jugadorId, roomCode}` | `gameState` final | ✅ **NUEVO**. El jugador que emite este evento pierde. El backend declara ganador al rival, guarda estadísticas en BD y difunde el estado final de la partida. |

### 4.3. El objeto `gameState`
Cualquier acción en el tablero (Combate) hace que el servidor responda emitiendo un evento `gameState` a ambos clientes con el JSON completo y actualizado:
- Identidad y vida actual de los jugadores.
- Matrices de tablero oscurecidas o actualizadas.
- Variable `fase` (`COLOCACION`, `COMBATE`, `TERMINADO`).
- Variable `turnoActualId` y `tiempoRestante`.
