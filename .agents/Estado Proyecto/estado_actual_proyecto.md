# 📊 Estado Actual del Proyecto — Warhammer Battleship

> **Actualizado:** 2026-05-12  
> **Fuentes de referencia:** Código fuente real del Backend y Frontend, documentación en `.agents`, historial de conversaciones  
> **Método:** Auditoría completa del repositorio.

---

## 1. Resumen Ejecutivo

| Área | Estado |
|------|--------|
| **Seguridad JWT + Middleware (Fingerprinting)** | ✅ Implementado y funcional |
| **Gestión de Usuarios (CRUD) y Email** | ✅ Implementado |
| **Configuración BBDD Híbrida (MySQL + Mongo)** | ✅ Completado |
| **Migración de Entidades (nuevo esquema)** | ✅ Completado |
| **Motor de Juego (GameEngine)** | ✅ Completo — 4 personajes, pasivas y habilidades activas (12 SKL) |
| **Persistencia del Juego en Tiempo Real** | ✅ Completo — `GameSocketController` guarda en MySQL y Mongo |
| **Timer de turno sincronizado** | ✅ Completo — `TurnTimerService` integrado vía `GameRoomManager.startTimer()` |
| **Evento `rendirse`** | ✅ Implementado — `onRendirse()` en `GameSocketController` |
| **Endpoint REST de Estadísticas** | ✅ **`EstadisticasController` CREADO** (`/api/estadisticas/jugador/{username}`) |
| **Frontend — resolución dinámica de host** | ✅ `SocketService` y `AuthService` usan `window.location.hostname` |
| **Archivos de Despliegue (Docker)** | ✅ `docker-compose.yml` presente en la raíz del repositorio |
| **Bugs de flujo de turnos** | ⚠️ Pendiente — doble turno observado en logs |
| **Habilidades con errores** | ⚠️ Pendiente — `SKL_WUL_1` (Desafío del Errante) y `SKL_WUL_2` (Colmillo) sin confirmar |
| **Seguridad WebSockets** | ⚠️ Sin filtro JWT — tráfico WS directo al `GameSocketController` |
| **Token de recuperación de contraseña** | ⚠️ Se expone en la URL — requiere solución |
| **Diseño Responsive** | ⚠️ Pendiente — la UI no está adaptada a pantallas pequeñas |
| **Segunda partida tras finalizar** | ❌ Bug conocido — no se puede unir a una nueva sala |

> [!NOTE]
> La deuda técnica ya no bloquea el flujo principal del juego. La prioridad ahora es **corrección de bugs** y **experiencia de usuario**.

---

## 2. Auditoría del Código Backend

### ✅ 2.1 Migración de Base de Datos Híbrida
- `application.properties` configurado con URI de MySQL (`localhost:3306/prueba_prfinal`) y MongoDB (`localhost:27017/batalla_naval_stats`).
- `JpaConfig` y `MongoConfig` segregan los repositorios para evitar conflictos de autoconfiguración.

### ✅ 2.2 Entidades e Interfaces
- `UsuarioEntity` usa `username` y `passwordHash`.
- `PartidaEntity` relaciona Host y Ganador (`UsuarioEntity`), con `fechaInicio`/`fechaFin` y `EstadoPartida`.
- Entidades de juego: `PersonajeEntity`, `BarcosCatalogoEntity`, `PersonajeFlotaEntity`.
- **MongoDB**: `PartidaStatsDocument` + `EstadisticasRepository`.

### ✅ 2.3 Motor del Juego — `GameEngine`
Motor completamente funcional con lógica de reglas aislada (sin dependencias de Spring):
- `procesarDisparo()` — gestiona impactos, fallos, hundimiento (DFS), pasivas de personajes.
- `usarHabilidad()` — router hacia 12 habilidades activas (3 por personaje × 4 personajes).
- **Pasivas implementadas**: `PAS_WUL` (tiro extra), `PAS_AIS` (ignora escudos 20%), `PAS_LOK` (revela adyacente al hundir), `PAS_ARA` (esquive 20%).
- **Habilidades activas**: Todas implementadas en `ejecutarEfectoHabilidad()` con lógica real.

### ✅ 2.4 Timer de Turno Sincronizado
- `TurnTimerService` corre en hilo background, descuenta 1 segundo/tick y difunde `gameState` por WebSocket.
- `GameRoomManager` lo instancia con `startTimer(roomCode)` al pasar a fase `COMBATE`.
- `removeRoom()` detiene el timer y elimina ambos maps (`activeRooms` + `activeTimers`) para evitar fugas de memoria.

### ✅ 2.5 Evento `rendirse`
- `onRendirse()` en `GameSocketController`: detecta el rendido, declara ganador al rival, difunde estado final y llama a `limpiarSalaFinalizada()`.

### ✅ 2.6 EstadisticasController — CREADO
```java
@GetMapping("/jugador/{username}")
public ResponseEntity<StatsAgregadasDTO> getEstadisticasJugador(@PathVariable String username)
```
El endpoint ya existe en `api.controller.EstadisticasController`. La petición del frontend (`/api/estadisticas/jugador/{username}`) debería resolverse correctamente.

### ⚠️ 2.7 Bugs Conocidos del Backend
- **Flujo de turnos**: Los logs muestran que el mismo jugador puede atacar dos veces seguidas en ciertos escenarios. Relacionado con `haAtacadoEsteTurno` / `turnoExtraWulfrik`.
- **Sala zombie tras segunda partida**: `GameRoomManager` no reinicia el estado correctamente al crear una segunda partida en la misma sala.

---

## 3. Estado del Frontend

| Componente | Estado |
|---|---|
| `AuthService` | ✅ Fingerprinting, sliding session, resolución dinámica de URL |
| `SocketService` | ✅ Resolución dinámica `localhost` vs producción; evento `rendirse` implementado |
| `StatsDTO` | ✅ Refactorizada para `username`, `hitsAcertados`, `barcosHundidos` |
| Login / Register / Update | ✅ JSON con `username` |
| Petición estadísticas | ✅ Llama a `withMiddlewareToken` → `GET /api/estadisticas/jugador/{username}` |
| Habilidades con coordenadas | ✅ `usarHabilidad(id, x, y)` emite las coordenadas requeridas |
| Abandono / rendición | ✅ `rendirse()` y `disconnect()` implementados |
| Token reset-password en URL | ⚠️ El token temporal aparece en la URL — riesgo de exposición |
| Responsive / móvil | ⚠️ Layout no adaptado a pantallas pequeñas |
| Segunda partida | ❌ No se puede unir a una nueva sala tras terminar una partida |

---

## 4. Arquitectura de Despliegue en Producción

| Infraestructura | Estado |
|---|---|
| **`docker-compose.yml`** | ✅ Presente en la raíz del repositorio |
| **Servicios definidos** | MySQL 8.0, MongoDB 7.0, Backend Spring Boot, Frontend Angular/Nginx |
| **Variables de entorno Docker** | ✅ Configuradas (sobreescriben `application.properties` en contenedor) |
| **`Dockerfile` Backend** | ⚠️ Pendiente de validar (existe directorio, falta confirmar) |
| **`Dockerfile` Frontend (Nginx)** | ⚠️ Pendiente de validar |
| **Credenciales en texto plano** | ⚠️ `application.properties` tiene contraseñas en claro — usar variables de entorno |

---

## 5. Próximos Pasos Recomendados (Prioridades)

| Prioridad | Acción | Detalle |
|---|---|---|
| 🔴 **URGENTE** | **Corregir flujo de turnos** | Revisar la lógica de `haAtacadoEsteTurno` en `GameEngine` y el doble disparo de Wulfrik. |
| 🔴 **URGENTE** | **Corregir bug segunda partida** | `GameRoomManager` debe resetear el `GameEngine` de la sala al terminar para permitir nuevas partidas. |
| 🟡 Media | **Proteger token reset-password** | No incluir el token JWT en la URL; usar un parámetro de formulario o cuerpo POST. |
| 🟡 Media | **Validar Dockerfiles** | Confirmar que los `Dockerfile` del backend y frontend compilan correctamente. |
| 🟡 Media | **Seguridad WebSockets** | Añadir validación de JWT en la conexión inicial de Socket.IO (handshake auth). |
| 🟡 Media | **Habilidades Wulfrik** | Verificar `SKL_WUL_1` y el rango de `SKL_WUL_2` (línea de 3 celdas centrada). |
| 🟢 Baja | **Diseño Responsive** | Adaptar tableros y modales a pantallas pequeñas y táctiles. |
| 🟢 Baja | **Mensajes de acción** | Revisar y limpiar los mensajes de estado mostrados en partida. |
