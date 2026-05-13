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

### ~~🟢 Diseño no responsive~~
**RESUELTO (2026-05-12).** Se han añadido media queries en `partida-activa.component.css` para adaptar la interfaz a tablets y móviles. Se ha implementado un diseño a pantalla completa para dispositivos móviles, estructurado íntegramente con `flexbox` dinámico y bloqueando cualquier scroll nativo de la página mediante `position: fixed` en el wrapper. Para evitar que componentes o modales se corten si la pantalla es muy corta, se ha dotado al contenedor principal y a todos los modales de un scroll interno inteligente (`overflow-y: auto` y `max-height: 90dvh`). El tablero central conserva su tamaño máximo ocupando fluidamente los espacios laterales, y se le han aplicado reglas de flexbox estrictas (`align-self: center`, `justify-self: center` y `margin: 0 auto`) para clavarlo en el mismísimo centro geométrico de la pantalla de forma absoluta, manteniendo íntegramente las dimensiones deseadas.

---

## ⚠️ PENDIENTES

EL boton de jugar desaparece con la resolucion demasiado alta. Deberia desaparecer sobre 768px

Opciones para la navbar:
- A partir de la pantalla creacion de sala deberia desaparecer la nav bar. Quitarla.
- O dejarla y ajustar que pasa en la sala y sockets en base a si un jugador le da a algun boton del menu y se sale. 

Boton de salir de sala en movil deberia estar debajo, igual que en el perfil. 

Las salas no se ordenan por fecha de creacion. 

Si multiples jugadores meten solicitud a la misma sala y el admin acepta a uno, los otros dos siguen viendo la solicitud como pendiente con el mensaje de esperando. Hice una prueba con 4 solicitudes, se acepto a jugador 1.  Jugador 2 recibio el mensaje de el admin rechazo la solicitud, el resto quedan con el mensaje de esperando a que el admin acepte.


Hay que verificar que el token se borre en el momento que el jugador abandona el juego de cualquier forma. Tanto como si se va cerrando la pestaña, como si le da a salir desde el menu. O si pone la url directamente. Tambien habria que comprobar que si se mete a un enlace directamente sin ese token le redirija al login. 

Terminar de hacer a ikit.

Textos de habilidades desbordan. Sobretodo el de habilidad pasiva.

Habilidad aranesa disparo de saloma no funciona como se describe. La pasiva de aranesa se activa demasiado seguido. 

-RF1 - El logo aparece en el menu encima de los personajes cuando la resolucion es pequeña pero que no llegua a movil. 

Quitar que el logo sea un boton.

En resolución de movil desborda la imagen del menu principal, hay que bloquear que no haga scroll y bajar un poco mas los personajes de sitio. Esto relacionado con -RF1- Puede que el logo que aparezca sea el que aparece en el movil. Ajustar las resoluciones responsive. Tambien se puede hacer scroll de la pantalla en lista de salas. Que no haya la opcion de hacer scroll fuera del cuadro para buscar las salas. Lo mismo para la pantalla de seleccion de personaje. que no haya tanto padding entre los botones y la imagen. 

Comprobar funcionalidad del boton de abandonar y como interactua con los sockets en los otros jugadores.

Quitar la navbar en la pantalla de juego y en la seleccion de personaje. 


ERROR CRITICO:
Las partidas solo se crean cuando se termina, no se crean cuando se le da a crear sala en el boton sala. Por tanto no se actualizan los estados de en espera y jugando. Solo se usa finalizada. 

--PLANTEAMIENTO-- Que funcion recibe la tabla partida stats. El volcado en mongo funciona, pero los datos de sql no almacenan nada

### 🟡 Token de recuperación de contraseña en la URL
**Síntoma**: El enlace de reset-password incluye el JWT en la query string (visible en el historial del navegador y logs de servidor).  
**Solución propuesta**: Enviar el token en el cuerpo del formulario POST en lugar de como parámetro GET en la URL.

---

### 🟡 Seguridad WebSockets sin autenticación JWT
**Estado**: La conexión Socket.IO al puerto 8081 no pasa por ningún filtro de seguridad. Cualquier cliente puede emitir eventos de juego.  
**Solución propuesta**: Añadir validación del JWT en el `handshake` de Socket.IO (parámetro `auth` del cliente) y verificarlo en el `onConnect` del servidor.

---

---

## 📋 Conectividad Middleware — Resumen

| Capa | Estado |
|---|---|
| HTTP `/api/**` | ✅ Pasa por `JwtFilter` + Fingerprint en todas las peticiones |
| `/api/auth` y `/api/lobby` | ✅ Controladores en paquete `middleware` |
| `/api/personajes` y `/api/estadisticas` | ⚠️ Controladores en paquete `api` (fuera del middleware) |
| WebSockets (puerto 8081) | ❌ Sin filtro de seguridad — acceso directo a `GameSocketController` |
