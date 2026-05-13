# 🐛 Log de Errores — Warhammer Battleship

> **Actualizado:** 2026-05-13

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

### ~~Habilidades de Ikit Claw y Sincronización de Feedback~~
**RESUELTO (2026-05-13).**
- **Lógica de Habilidades**: Implementado el Rayo de Piedra Bruja (impacto + revelado en cruz) y el Cohete de Muerte (área 3x3 con contador de barcos hundidos).
- **Pasiva**: Probabilidad de bypass de cooldown aumentada al **20%**.
- **Feedback**: Las habilidades ahora informan correctamente de los barcos **HUNDIDOS**, corrigiendo la sobrescritura de mensajes en `GameEngine.java`.
- **Revelado de Agua**: Las habilidades de visión ahora marcan el agua como `AGUA_GOLPEADA`.
- **Nombres**: Sincronizados todos los nombres de habilidades (ej: "Piedra Bruja") entre Backend y Frontend.

---

### ~~Habilidad Disparo de Saloma (Aranessa) y Bruma Marina (Aislinn)~~
**RESUELTO (2026-05-13).**
- **Disparo de Saloma**: Corregida para que realmente "destruya nieblas" revelando el área 2x2 de impacto, además de eliminar TODOS los escudos del rival (individuales y totales) antes del disparo. Añadido mensaje de feedback explícito: *"ha DESTROZADO todas las defensas y nieblas enemigas"*.
- **Bruma Marina**: Refactorizada para usar un área 2x2 (con targeting) en lugar de 4 celdas aleatorias, cumpliendo con su descripción.
- **Sincronización**: Actualizadas las descripciones en `CharacterFactory.java` y `seleccion-personajes.component.ts` para que sean precisas y consistentes.

---

### ~~Ocultar Navbar en pantallas críticas de juego / Textos desbordados~~
**RESUELTO (2026-05-13).**
- **Navbar**: Desactivada en `/partida`, `/seleccion-personajes` y `/partida-activa` para mejorar la inmersión.
- **Scroll y Desbordamientos**: 
  - Se ha bloqueado el scroll de la página en `Menu`, `Lista de Salas`, `Selección de Personaje` y `Partida Activa` usando `position: fixed` y `overflow: hidden`.
  - En móviles, se ha reducido el tamaño del logo y ajustado márgenes (`vh`) para que los personajes bajen de posición y no se solapen.
  - En tablets (768px-1024px), se ha ocultado el logo duplicado (mobile-logo) para que solo se vea el de la Navbar.
- **Logo**: Se ha eliminado el cursor de mano (`pointer`) del logo para dejar claro que no es un botón.
- **Overflow**: Aumentado el ancho de tooltips en CSS (240px) y añadida regla `word-wrap` para evitar que las descripciones largas se corten.

---

### ~~Visualización de Habilidades Defensivas en el Tablero~~
**RESUELTO (2026-05-13).**
- Implementado sistema de iconos dinámicos para casillas protegidas.
- **Escudos individuales**: Se muestra un icono de escudo (🛡️) flotante sobre las casillas protegidas por habilidades de Wulfrik, Aislinn o Lokhir.
- **Escudo total**: Cuando Aranessa activa su protección global, todo el tablero del jugador muestra un aura azulada y bordes resaltados para indicar la invulnerabilidad.
- Actualizada la función `getClaseCasilla` para procesar coordenadas y estados de protección en tiempo real desde el `gameState`.

---

## ⚠️ PENDIENTES

---

### ~~Botón de Jugar desaparecía en resoluciones intermedias~~
**RESUELTO (2026-05-13).**
- Corregida media query en `menu.component.css`. El botón ahora es visible hasta los 768px (antes desaparecía a los 1024px).
- En tablets (768px-1024px), el botón se reposiciona debajo del logo y el perfil para evitar solapamientos, manteniendo su funcionalidad.

---


---

### ~~Posición del botón Salir en móvil (Lobby)~~
**RESUELTO (2026-05-13).**
- Reorganizado el grid de `partida.css` para resoluciones móviles (< 600px).
- El botón "Salir" ahora ocupa todo el ancho en la parte inferior de la pantalla, manteniendo la consistencia con el diseño del componente Perfil.
- Se han reubicado los ajustes y los botones de inicio para una mejor jerarquía visual en dispositivos táctiles.

---

---

### ~~Las salas no se ordenan por fecha de creacion~~
**RESUELTO (2026-05-13).**
- Añadido campo `fechaCreacion` a `LobbyRoom` en el Backend.
- Modificado `LobbyManager.java` para inicializar la fecha al crear la sala y devolver la lista de salas ordenada por este campo (descendente: más recientes primero).

---

---

### ~~Sincronización de solicitudes de unión al lobby~~
**RESUELTO (2026-05-13).**
- Implementado sistema de rastreo de solicitudes pendientes en `SocketService.java`.
- Cuando el administrador acepta a un jugador, todos los demás aspirantes a esa misma sala reciben automáticamente un mensaje de "Solicitud rechazada" (sala llena), evitando que se queden en espera indefinida.
- También se limpian las solicitudes si la sala se cierra o el administrador se desconecta.

---


Hay que verificar que el token se borre en el momento que el jugador abandona el juego de cualquier forma. Tanto como si se va cerrando la pestaña, como si le da a salir desde el menu. O si pone la url directamente. Tambien habria que comprobar que si se mete a un enlace directamente sin ese token le redirija al login. 


Comprobar funcionalidad del boton de abandonar y como interactua con los sockets en los otros jugadores.


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

### ?? Bot�n de uni�n no se bloquea en partidas activas
**S�ntoma**: En la lista de salas, el bot�n 'Unirse' permanece activo incluso si la partida ya ha comenzado (estado: JUGANDO), lo que permite enviar solicitudes de uni�n inv�lidas.
**Soluci�n propuesta**: Deshabilitar o cambiar el texto del bot�n cuando el estado de la sala no sea ESPERANDO.

