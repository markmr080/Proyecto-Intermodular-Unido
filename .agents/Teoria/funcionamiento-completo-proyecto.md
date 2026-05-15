# Funcionamiento Completo del Proyecto — Warhammer Battleship

Juego de batalla naval multijugador en tiempo real ambientado en el universo Warhammer. Dos jugadores se enfrentan en una partida por turnos usando personajes únicos con habilidades especiales.

---

## 1. Visión General del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                        DOCKER COMPOSE                           │
│                                                                 │
│  ┌──────────────┐   ┌──────────────┐   ┌────────────────────┐  │
│  │  MySQL :3306 │   │ MongoDB:27017│   │  Backend :8080/:8081│  │
│  │  (usuarios,  │   │  (stats de  │   │  Spring Boot +      │  │
│  │  partidas,   │   │   partidas) │   │  Socket.IO          │  │
│  │  personajes) │   └──────────────┘   └────────────────────┘  │
│  └──────────────┘                               │               │
│                                                 │ HTTP/WS       │
│  ┌──────────────────────────────────────────────┘               │
│  │                                                              │
│  ▼                                                              │
│  ┌──────────────────────────────┐                               │
│  │   Frontend Angular :8082     │                               │
│  │   (Nginx en producción)      │                               │
│  └──────────────────────────────┘                               │
└─────────────────────────────────────────────────────────────────┘
```

### Puertos
| Puerto | Servicio |
|--------|----------|
| `3306` | MySQL (base de datos relacional) |
| `27017` | MongoDB (estadísticas) |
| `8080` | Backend REST API (Spring Boot) |
| `8081` | Backend WebSocket (Socket.IO) |
| `8082` | Frontend Angular (en Docker/producción) |

---

## 2. Stack Tecnológico

### Backend — Java / Spring Boot
- **Spring Boot** — framework principal
- **netty-socketio** — servidor Socket.IO integrado en Spring
- **Spring Data JPA + MySQL** — persistencia de usuarios, partidas y personajes
- **Spring Data MongoDB** — estadísticas de partidas (hits, barcos hundidos...)
- **JWT (JSON Web Tokens)** — autenticación stateless
- **Spring Security** — filtros de seguridad en endpoints REST

### Frontend — Angular (TypeScript)
- **Angular 17+** — framework SPA con lazy loading por rutas
- **socket.io-client** — comunicación en tiempo real
- **RxJS Subjects** — bus de eventos reactivo interno
- **Vanilla CSS** — estilos sin frameworks externos

---

## 3. Estructura de Carpetas

### Backend
```
Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/
└── com/cifpaviles/proyectofinal/CLMM/
    ├── ClmmApplication.java          ← Punto de entrada Spring Boot
    ├── api/
    │   ├── config/
    │   │   ├── SocketIOConfig.java   ← Configura el servidor Socket.IO (puerto, CORS, JWT)
    │   │   └── DataInitializer.java  ← Puebla MySQL con barcos y flotas al arrancar
    │   ├── controller/
    │   │   └── GameSocketController.java  ← Eventos WebSocket del JUEGO
    │   ├── model/
    │   │   ├── entity/               ← Entidades JPA (MySQL)
    │   │   └── game/                 ← Modelos en memoria (GameState, Player, Skill...)
    │   └── service/
    │       └── game/
    │           ├── GameEngine.java        ← Motor de reglas del juego
    │           ├── GameRoomManager.java   ← Gestiona salas activas en memoria
    │           ├── CharacterFactory.java  ← Crea personajes con sus habilidades
    │           └── TurnTimerService.java  ← Cronómetro de turno por sala
    └── middleware/
        ├── config/security/          ← JWT, filtros, SecurityConfig
        └── sockets/
            ├── SocketService.java    ← Eventos WebSocket del LOBBY
            ├── LobbyManager.java     ← Salas de espera en memoria
            └── LobbyRoom.java        ← Modelo de sala de lobby
```

### Frontend
```
front-conectado-main/src/app/
├── services/
│   ├── socket.service.ts    ← ÚNICO punto de contacto con WebSocket
│   ├── auth.service.ts      ← Login, registro, JWT, perfil
│   └── room.service.ts      ← Llamadas REST de salas
├── login/                   ← Pantalla de login
├── registro/                ← Pantalla de registro
├── menu/                    ← Menú principal con carousel de personajes
├── lista-salas/             ← Listado de partidas disponibles
├── partida/                 ← Lobby de sala (sala de espera)
├── seleccion-personajes/    ← Elección de personaje antes de jugar
├── partida-activa/          ← Pantalla de combate (el juego en sí)
├── perfil/                  ← Perfil del usuario con estadísticas
└── reset-password/          ← Recuperación de contraseña
```

---

## 4. Autenticación — Flujo JWT

```
Usuario           AuthService.ts         Backend REST (:8080)
   │                    │                        │
   │── login(user,pass)─►│                        │
   │                    │── POST /api/auth/login ─►│
   │                    │                        │ valida credenciales
   │                    │◄── { token: "JWT..." } ─│
   │                    │ guarda en sessionStorage │
   │◄── navega a /menu  │                        │
   │                    │                        │
   │  (peticiones REST  │                        │
   │   con header:      │── GET /api/... ─────────►│
   │   Authorization:   │   + Bearer {token}     │ JwtFilter valida token
   │   Bearer {token})  │◄── respuesta ───────────│
```

- El token JWT se guarda en **`sessionStorage`** (no localStorage), por seguridad de sesión.
- El **`JwtFilter`** intercepta todas las peticiones REST y valida el token antes de procesarlas.
- La conexión WebSocket también valida el JWT en el **handshake** (parámetro `?token=...`).
- El **`authGuard`** protege las rutas Angular (`/menu`, `/partida`, `/partida-activa`, etc.).

---

## 5. Bases de Datos

### MySQL — Datos relacionales
| Tabla | Contenido |
|-------|-----------|
| `usuarios` | id, username, email, password_hash, role, profile_picture |
| `partidas` | id, jugador1, jugador2, ganador, estado, fechaInicio, fechaFin |
| `personajes` | id, nombre, descripcion |
| `barcos_catalogo` | id, nombre, tamaño |
| `personaje_flota` | personaje_id, barco_id, cantidad (flota única por personaje) |

### MongoDB — Estadísticas (colección `partida_stats`)
Documento por jugador por partida:
```json
{
  "idPartida": 42,
  "idUsuario": 7,
  "hitsAcertados": 12,
  "hitsFallados": 8,
  "barcosHundidos": 3,
  "username": "Marcos"
}
```

### DataInitializer — Datos de referencia
Al arrancar, el backend puebla automáticamente las tablas si están vacías:

| Personaje | Flota (tamaños) | Total celdas |
|-----------|----------------|--------------|
| Wulfrik   | 5, 4, 3, 3, 2  | 17           |
| Aislinn   | 5, 4, 3, 2, 2  | 16           |
| Lokhir    | 5, 3, 3, 2, 2  | 15           |
| Aranessa  | 4, 4, 3, 3, 2  | 16           |
| Ikit Claw | (custom)       | —            |
| B. Gelt   | (custom)       | —            |

---

## 6. Flujo Completo de una Partida

### Fase 1 — Lobby
```
J1 (anfitrión)                    Servidor                    J2 (invitado)
      │                               │                              │
      │── POST /api/partidas ─────────►│ crea sala en MySQL           │
      │◄── { codigoSala: "ABC123" } ──│                              │
      │── emit('registrar-usuario') ──►│                              │
      │                               │◄─ emit('solicitar-unirse') ──│
      │◄── event('nueva-solicitud') ──│                              │
      │── emit('aceptar-solicitud') ──►│                              │
      │                               │── event('solicitud-aceptada')►│
      │◄── event('jugador-unido') ────│                              │
```

### Fase 2 — Selección de Personaje
```
J1                        SocketService.java                      J2
 │── emit('seleccionar-personaje', {personajeId}) ─────────────────►│
 │                                (reenvía al otro)                 │
 │◄─────────────────── event('personaje-seleccionado') ─────────────│
```

Cuando el anfitrión pulsa "Comenzar":
```
J1 ─► emit('comenzar-juego') ─► servidor ─► event('juego-comenzado') ─► J1 y J2
```
Ambos navegan a `/partida-activa/:code`.

### Fase 3 — Colocación de Barcos
```
J1                        GameSocketController                      J2
 │── emit('join-room', {jugadorId, personajeId}) ──────────────────►│
 │                        crea GameState en memoria                  │
 │◄──────────────────── event('gameState') ─────────────────────────│
 │                                                                   │
 │── emit('colocar-barcos', {tablero[][]}) ─────────────────────────│
 │         (cuando ambos están listos → fase COMBATE)               │
 │◄── event('gameState') ────────────────────────────────────────────│
```

### Fase 4 — Combate (por turnos)
```
Jugador activo             GameSocketController / GameEngine        Rival
      │── emit('atacar', {x, y}) ─────────────────────────────────►│
      │               procesarDisparo()                              │
      │               - comprueba escudos                           │
      │               - aplica pasivas                              │
      │               - actualiza CellStatus                        │
      │               - comprueba victoria                          │
      │               - cambia turno                                │
      │◄──────── event('gameState') → AMBOS jugadores ──────────────│
```

### Cronómetro de turno (TurnTimerService)
- Se activa al pasar a fase **COMBATE**.
- Cada segundo descuenta `tiempoRestante` en `GameState` y difunde el estado.
- Si llega a 0, cambia el turno automáticamente.
- Al finalizar la partida, el timer se detiene y libera el hilo.

### Fin de partida
1. `GameEngine` detecta `vidas == 0` en algún jugador → `juegoActivo = false`, `ganadorId = X`.
2. `difundirEstado()` envía el estado final a ambos clientes.
3. `limpiarSalaFinalizada()` guarda stats en BD y programa la eliminación de sala tras 10 s.

---

## 7. Modelos en Memoria (durante la partida)

### GameState — Estado global de la partida
```
GameState {
  jugador1: Player
  jugador2: Player
  turnoActualId: String       ← ID del jugador que debe actuar
  tiempoRestante: int         ← segundos del cronómetro (60 normal, 20 reacción)
  fase: String                ← "COLOCACION" | "COMBATE" | "FIN"
  juegoActivo: boolean
  ganadorId: String
  mensajeEstado: String       ← texto descriptivo de la última acción
  idPartidaMysql: Long        ← referencia a la BD
}
```

### Player — Jugador
```
Player {
  id: String
  nombre: String
  tablero: CellStatus[10][10]     ← estado de cada celda
  vidas: int                      ← celdas BARCO restantes (sin escudo)
  personaje: GameCharacter        ← habilidades y pasivas
  escudoCasillas: Set<String>     ← celdas protegidas "x,y"
  escudoTotalActivo: boolean      ← invulnerabilidad total (Aranessa)
  turnoExtraWulfrik: boolean      ← segundo disparo tras acierto
  habilidadUsadaEsteTurno: boolean
  haAtacadoEsteTurno: boolean
  hitsAcertados, hitsFallados, barcosHundidos: int  ← stats
}
```

### CellStatus — Estado de cada celda del tablero
| Valor | Significado | ¿Visible para el enemigo? |
|-------|-------------|--------------------------|
| `AGUA` | Mar vacío | Sí (sin marcar) |
| `BARCO` | Barco intacto | **No** (oculto) |
| `REVELADA` | Barco descubierto por habilidad de visión | **Sí** (👁️ ámbar) |
| `TOCADO` | Celda de barco impactada | Sí (🔥) |
| `HUNDIDO` | Barco completamente destruido | Sí (💀) |
| `AGUA_GOLPEADA` | Disparo fallido | Sí (💧) |

> **Importante:** el filtrado de `BARCO` → mostrar como `AGUA` en el tablero enemigo lo hace el **frontend**, no el backend. El backend envía todo el `GameState` y Angular decide qué mostrar según si la celda es propia o enemiga.

---

## 8. Personajes y Habilidades

Cada personaje tiene: **1 pasiva** + **3 habilidades activas** (2 ofensivas + 1 defensiva).

### Pasivas (automáticas, no requieren acción)
| Pasiva | Personaje | Efecto |
|--------|-----------|--------|
| `PAS_WUL` | Wulfrik | Al acertar un BARCO → turno extra (segundo disparo) |
| `PAS_AIS` | Aislinn | 20% de probabilidad de esquivar impactos entrantes |
| `PAS_LOK` | Lokhir | Al hundir un barco → revela una celda enemiga (`REVELADA`) |
| `PAS_ARA` | Aranessa | (especial de flota balanceada) |

### Habilidades activas
| ID | Personaje | Tipo | Efecto |
|----|-----------|------|--------|
| SKL_WUL_1 | Wulfrik | Ofensiva | Revela un BARCO enemigo aleatorio (→ `REVELADA`) |
| SKL_WUL_2 | Wulfrik | Ofensiva | 3 disparos en horizontal desde (x,y) |
| SKL_WUL_3 | Wulfrik | Defensiva | Escuda 1 celda BARCO propia aleatoria |
| SKL_AIS_1 | Aislinn | Ofensiva | 2 disparos aleatorios al enemigo |
| SKL_AIS_2 | Aislinn | Ofensiva | Cruz de 5 celdas centrada en (x,y) |
| SKL_AIS_3 | Aislinn | Defensiva | Escuda área 2×2 en tablero propio |
| SKL_LOK_1 | Lokhir | Ofensiva | 3 disparos en diagonal desde (x,y) |
| SKL_LOK_2 | Lokhir | Ofensiva | Revela celdas BARCO en área 3×3 |
| SKL_LOK_3 | Lokhir | Defensiva | Escuda el Arca Negra (barco tamaño 5) |
| SKL_LOK_4 | Lokhir | Ofensiva | **Venganza**: 5 disparos aleatorios (reemplaza SKL_LOK_3 al hundir el Arca) |
| SKL_ARA_1 | Aranessa | Ofensiva | Disparo + propagación a 4 adyacentes si impacta BARCO |
| SKL_ARA_2 | Aranessa | Ofensiva | Dispara a las 4 esquinas del tablero |
| SKL_ARA_3 | Aranessa | Defensiva | Escudo total para el siguiente turno completo |
| SKL_IKT_1 | Ikit Claw | Ofensiva | Rayo: impacta y revela adyacentes en cruz |
| SKL_IKT_2 | Ikit Claw | Ofensiva | Área masiva 3×3 |
| SKL_IKT_3 | Ikit Claw | Defensiva | Escuda área aleatoria 2×2 |
| SKL_GEL_1 | B. Gelt | Ofensiva | Transmutación: área 2×2 (daño + reveal) |
| SKL_GEL_2 | B. Gelt | Ofensiva | Lluvia de metal: 3 disparos aleatorios |
| SKL_GEL_3 | B. Gelt | Defensiva | Escuda el barco más grande del jugador |

### Reglas de uso de habilidades
- Solo **1 habilidad por turno** (`habilidadUsadaEsteTurno`).
- Las **ofensivas** terminan el turno (se llama a `cambiarTurno()` automáticamente).
- Las **defensivas** no terminan el turno (el jugador puede seguir atacando).
- Cada habilidad tiene un **cooldown** (número de turnos de espera entre usos).

---

## 9. Mecánica de Escudos

### Escudo de casilla (SKL_WUL_3, SKL_AIS_3, SKL_IKT_3, SKL_GEL_3...)
- Se añade la coordenada `"x,y"` al `Set<String> escudoCasillas` del Player.
- Cuando un disparo impacta esa celda, el escudo **absorbe el golpe**: la celda permanece como `BARCO`, las vidas NO bajan.
- El escudo de esa casilla se elimina tras absorber el impacto.
- **Lokhir (SKL_LOK_3 — Arca Negra):** si cualquier casilla del Arca es impactada, todos los escudos del Arca caen a la vez.

### Escudo total (SKL_ARA_3 — Aranessa)
- `escudoTotalActivo = true` en el Player.
- El siguiente ataque del rival es completamente ignorado (sin ningún cambio en el tablero).
- Se desactiva tras absorber ese turno.

---

## 10. Reconexión y Desconexiones

Si un jugador pierde la conexión durante una partida activa:

1. `onDisconnect` detecta que el jugador estaba en una sala activa.
2. Notifica al rival con `event('jugador-desconectado')`.
3. Inicia un **timer de gracia de 30 segundos**.
4. Si el jugador vuelve antes de 30s → se cancela el timer, se notifica con `event('jugador-reconectado')`.
5. Si no vuelve → el jugador desconectado pierde automáticamente. Su rival recibe el `gameState` final.

---

## 11. Eventos Socket.IO — Referencia Completa

### Lobby (SocketService.java)
| Dirección | Evento | Datos | Descripción |
|-----------|--------|-------|-------------|
| C → S | `registrar-usuario` | userId | Asocia el socket al userId |
| C → S | `solicitar-unirse` | {codigoSala, requesterId...} | J2 pide entrar a la sala de J1 |
| C → S | `aceptar-solicitud` | {codigoSala, requesterId...} | J1 acepta la solicitud |
| C → S | `rechazar-solicitud` | requesterId | J1 rechaza la solicitud |
| C → S | `cerrar-sala` | codigoSala | J1 cierra su sala |
| C → S | `iniciar-partida` | {codigoSala} | J1 arranca el flujo de selección |
| C → S | `seleccionar-personaje` | {codigoSala, userId, personajeId} | Un jugador elige personaje |
| C → S | `comenzar-juego` | codigoSala | J1 da comienzo a la partida activa |
| C → S | `expulsar-jugador` | {codigoSala, targetId} | J1 expulsa a J2 |
| C → S | `abandonar-sala` | {codigoSala, userId} | Un jugador abandona |
| S → C | `nueva-solicitud` | data | J1 recibe notificación de que alguien quiere unirse |
| S → C | `solicitud-aceptada` | codigoSala | J2 es admitido |
| S → C | `solicitud-rechazada` | mensaje | J2 es rechazado |
| S → C | `jugador-unido` | data | J1 confirma que J2 entró |
| S → C | `sala-cerrada` | mensaje | La sala fue cerrada |
| S → C | `partida-iniciada` | codigoSala | Arrancar selección de personaje |
| S → C | `personaje-seleccionado` | data | El rival eligió personaje |
| S → C | `juego-comenzado` | codigoSala | Navegar a partida-activa |
| S → C | `partida-cancelada` | userId | J2 se fue; sala vuelve a ESPERANDO |
| S → C | `jugador-expulsado` | userId | J2 fue expulsado |

### Juego (GameSocketController.java)
| Dirección | Evento | Datos | Descripción |
|-----------|--------|-------|-------------|
| C → S | `join-room` | {jugadorId, jugadorNombre, roomCode, personajeId} | Unirse a la sala de juego |
| C → S | `colocar-barcos` | {jugadorId, roomCode, tablero[][]} | Enviar tablero colocado |
| C → S | `atacar` | {jugadorId, roomCode, x, y} | Disparo normal |
| C → S | `usar-habilidad` | {jugadorId, roomCode, habilidadId, x, y} | Usar habilidad |
| C → S | `rendirse` | {jugadorId, roomCode} | Rendición voluntaria |
| S → C | `gameState` | GameState completo | Difundido tras cada acción y cada segundo |
| S → C | `jugador-desconectado` | "jugadorId\|nombre" | Rival perdió conexión |
| S → C | `jugador-reconectado` | jugadorId | Rival volvió a conectar |
| S → C | `reconexion-expirada` | roomCode | El tiempo de gracia expiró |

---

## 12. Rutas de la Aplicación Angular

| Ruta | Componente | Requiere auth |
|------|------------|---------------|
| `/login` | LoginComponent | No |
| `/registro` | RegistroComponent | No |
| `/reset-password` | ResetPasswordComponent | No |
| `/menu` | MenuComponent | **Sí** |
| `/lista-salas` | ListaSalas | **Sí** |
| `/partida/:code` | Partida (Lobby) | **Sí** |
| `/seleccion-personajes` | SeleccionPersonajesComponent | **Sí** |
| `/partida-activa/:code` | PartidaActivaComponent | **Sí** |
| `/perfil` | Perfil | **Sí** |

---

## 13. Cómo Levantar el Proyecto

### Con Docker Compose (recomendado)
```bash
# Desde la raíz del proyecto
docker-compose up --build
```
Acceder en: `http://localhost:8082`

### En local (desarrollo)
```bash
# Terminal 1 — Backend
cd Proyecto-Final-Multiplataforma-BackApi-master
./mvnw spring-boot:run

# Terminal 2 — Frontend
cd front-conectado-main
npm install
npm run dev
```
- Frontend: `http://localhost:4200`
- API REST: `http://localhost:8080`
- WebSocket: `http://localhost:8081`
