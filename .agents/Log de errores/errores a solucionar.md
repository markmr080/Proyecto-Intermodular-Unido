# 🐛 Log de Errores — Warhammer Battleship

> **Actualizado:** 2026-05-12

---

## ✅ RESUELTOS

### ~~Timer de la sala no compartido entre jugadores~~
**RESUELTO (2026-05-08).** `TurnTimerService` difunde `gameState` por WebSocket cada segundo desde `GameRoomManager.startTimer()`.

---

### ~~El jugador ataca y no pasa de turno / timer se queda en 20s~~
**RESUELTO (2026-05-08).** Había dos bugs:

**Bug A — Frontend bloqueaba ataques durante `faseReaccion`:**
```typescript
// ❌ ANTES
if (... || this.gameState.faseReaccion) return;
// ✅ AHORA
if (this.gameState?.fase !== 'COMBATE' || !this.esMiTurno) return;
```

**Bug B — Backend llamaba siempre a `activarFaseReaccion()`:**
```java
// ❌ ANTES
if (state.isFaseReaccion()) { state.cambiarTurno(); }
else { activarFaseReaccion(); } // siempre llegaba aquí → timer=20s
// ✅ AHORA
state.cambiarTurno(); // timer=60s, turno al otro jugador
```
Archivos modificados: `GameEngine.java`, `partida-activa.component.ts`.

---

### ~~Falta EstadisticasController~~
**RESUELTO.** `EstadisticasController` creado en `api.controller`. Expone `GET /api/estadisticas/jugador/{username}` delegando en `EstadisticasService.getStatsAgregadas()`.

---

## ⚠️ PENDIENTES

Importante:
Hay que comprobar que pasa si uno de los jugadores cierra la ventana del explorador. Habria que mirar de en el momento que cierre el jugador la ventana, al otro le salga un modal con un timer de 30 segundos que ponga "nombre de jugador abandono la partida, la partida se cerrara en 30 segundos". Si se reconecta antes de los 30 segundos, se reanuda la partida. De lo contrario que tire al lobby y elimine la sala. El funcionamiento debe ser igual al boton de rendicion. El jugador que abandona le cuenta la partida como perdida y al que no, como ganada. 

### 🟡 Habilidad: `SKL_WUL_1` — Desafío del Errante no responde
**Síntoma**: La habilidad no produce efecto visible en el frontend.  
**Causa a investigar**: Posiblemente el frontend no envía las coordenadas `x,y` al usar la habilidad (`usarHabilidad(id, x=-1, y=-1)`), pero `ejecutarDesafioErrante` las necesita para determinar si fue agua o barco. (Error array index out of bounds)

---

### 🟡 Habilidad: `SKL_WUL_2` — Colmillo de los Mares — rango incorrecto
**Síntoma**: La línea de 3 casillas empieza en la celda seleccionada y va hacia la derecha (`y`, `y+1`, `y+2`). El comportamiento esperado es que la celda seleccionada sea el centro (`y-1`, `y`, `y+1`).  Tambien si hundes un barco pasa de turno automaticamente. Si hace encima de un barco de 3, se seleccionar la casilla izquierda y derecha. La central queda sin marcar. Se tendrian que marcar todas. Se puede deber a un conflicto con la habilidad pasiva de wulfrik.
**Archivo**: `GameEngine.java` → `ejecutarColmilloMares()`. Cambiar `dy=0..2` por `dy=-1..1`.

---

### 🟡 Token de recuperación de contraseña en la URL
**Síntoma**: El enlace de reset-password incluye el JWT en la query string (visible en el historial del navegador y logs de servidor).  
**Solución propuesta**: Enviar el token en el cuerpo del formulario POST en lugar de como parámetro GET en la URL.

---

### 🟡 Seguridad WebSockets sin autenticación JWT
**Estado**: La conexión Socket.IO al puerto 8081 no pasa por ningún filtro de seguridad. Cualquier cliente puede emitir eventos de juego.  
**Solución propuesta**: Añadir validación del JWT en el `handshake` de Socket.IO (parámetro `auth` del cliente) y verificarlo en el `onConnect` del servidor.

---

### 🟢 Diseño no responsive
**Síntoma**: La UI no está adaptada a pantallas pequeñas ni a dispositivos táctiles. Los tableros y modales se desbordan.  
**Acción**: Revisar breakpoints CSS en `partida-activa` y componentes de lobby.

---

### 🟢 Mensajes de acción en partida
**Síntoma**: Los mensajes de estado (`mensajeEstado`) no siempre se muestran correctamente en el frontend o son poco descriptivos.

---

---

### 🟢 Mensajes de acción en partida
**Síntoma**: Cuando un jugador se escuda con la habilidad de wulfrik Favor Ruinoso sale el mensaje de que casilla se ha protegido a ambos jugadores.

---

## 📋 Conectividad Middleware — Resumen

| Capa | Estado |
|---|---|
| HTTP `/api/**` | ✅ Pasa por `JwtFilter` + Fingerprint en todas las peticiones |
| `/api/auth` y `/api/lobby` | ✅ Controladores en paquete `middleware` |
| `/api/personajes` y `/api/estadisticas` | ⚠️ Controladores en paquete `api` (fuera del middleware) |
| WebSockets (puerto 8081) | ❌ Sin filtro de seguridad — acceso directo a `GameSocketController` |
