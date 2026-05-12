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

### ~~Mensajes de acción en partida / Errores Habilidades Wulfrik~~
**RESUELTO (2026-05-12).**
- **Mensajes**: Reescritos todos los mensajes en `GameEngine.java` para ser más descriptivos y menos técnicos.
- **Favor Ruinoso**: Eliminadas coordenadas del mensaje público para evitar fugas de información.
- **Colmillo de los Mares**: Rango corregido a `-1..1` (centrado).
- **Desafío del Errante**: Añadida validación de coordenadas para evitar `ArrayIndexOutOfBoundsException`.
- **aplicarDisparoHabilidad**: Refactorizado para devolver estados legibles.

---

---

## ⚠️ PENDIENTES

EL boton de jugar desaparece con la resolucion demasiado alta. Deberia desaparecer sobre 768px
Opciones para la navbar:
- A partir de la pantalla creacion de sala deberia desaparecer la nav bar. Quitarla.
- O dejarla y ajustar que pasa en la sala y sockets en base a si un jugador le da a algun boton del menu y se sale. 

Boton de salir de sala en movil deberia estar debajo, igual que en el perfil. 




---

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

---

## 📋 Conectividad Middleware — Resumen

| Capa | Estado |
|---|---|
| HTTP `/api/**` | ✅ Pasa por `JwtFilter` + Fingerprint en todas las peticiones |
| `/api/auth` y `/api/lobby` | ✅ Controladores en paquete `middleware` |
| `/api/personajes` y `/api/estadisticas` | ⚠️ Controladores en paquete `api` (fuera del middleware) |
| WebSockets (puerto 8081) | ❌ Sin filtro de seguridad — acceso directo a `GameSocketController` |
