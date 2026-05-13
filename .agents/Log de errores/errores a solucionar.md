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


### ~~Verificacion del borrado de token al salir y proteccion de rutas directas~~
**RESUELTO (2026-05-13).**
- **Cierre de pestaña**: El token y los datos de usuario se almacenan en sessionStorage, por lo que el navegador los borra automaticamente al cerrar la pestaña.
- **Salida manual**: El boton 'Salir' en el menu llama a authService.logout(), que elimina explicitamente el JWT de la sesion.
- **Acceso por URL directa**: Se ha implementado un AuthGuard y se ha aplicado en app.routes.ts a todas las rutas protegidas. Si se intenta acceder sin un token valido, el usuario es redirigido automaticamente a la pantalla de /login.


### ~~Funcionalidad del bot�n abandonar y sincronizaci�n de sockets~~
**RESUELTO (2026-05-13).**
- **Lobby (partida.ts)**: Verificado que al salir se emite bandonar-sala. J1 recibe partida-cancelada (limpia su J2) y J2 recibe sala-cerrada (lo expulsa).
- **Selecci�n de Personajes (seleccion-personajes)**: Se ha corregido un bug cr�tico donde J2 no escuchaba el evento sala-cerrada. Ahora, si el anfitri�n abandona durante la selecci�n, J2 recibe la notificaci�n, se limpia su sesi�n y vuelve al men�.
- **Partida Activa (partida-activa)**: El bot�n Rendirse emite el evento 
endirse, que finaliza la partida en el servidor, da la victoria al rival y cierra la sala tras 10 segundos de cortes�a.


### ~~Sincronización de PartidaEntity con el Lobby~~
**RESUELTO (2026-05-13).**
Las partidas ahora se registran en MySQL desde el momento en que se crean en el lobby (`EN_ESPERA`), pasan a `EN_CURSO` cuando se acepta un rival, y se actualizan a `FINALIZADA` al terminar. El ID de la base de datos se transmite en memoria a través de `LobbyRoom` y `GameState`.
### 🟡 Token de recuperación de contraseña en la URL
**Síntoma**: El enlace de reset-password incluye el JWT en la query string (visible en el historial del navegador y logs de servidor).  
**Solución propuesta**: Enviar el token en el cuerpo del formulario POST en lugar de como parámetro GET en la URL.

---

### ~~Seguridad WebSockets sin autenticación JWT~~
**RESUELTO (2026-05-13).**
- **Servidor**: Añadido un `AuthorizationListener` en `SocketIOConfig.java` que extrae el token JWT del parámetro GET (`?token=...`) durante el handshake y lo valida usando `JwtProvider`. Las conexiones sin token o con tokens inválidos son rechazadas.
- **Cliente**: Actualizado `socket.service.ts` para extraer el `auth_token` del `sessionStorage` e inyectarlo en la URL de conexión (`query: { token: ... }`).

---

---

## 📋 Conectividad Middleware — Resumen

| Capa | Estado |
|---|---|
| HTTP `/api/**` | ✅ Pasa por `JwtFilter` + Fingerprint en todas las peticiones |
| `/api/auth` y `/api/lobby` | ✅ Controladores en paquete `middleware` |
| `/api/personajes` y `/api/estadisticas` | ⚠️ Controladores en paquete `api` (fuera del middleware) |
| WebSockets (puerto 8081) | ✅ Protegido por `AuthorizationListener` (JWT en handshake) |

---

### ~~Pasiva de Lokhir (Saqueador Especialista) no funciona~~
**RESUELTO (2026-05-13).**
- **Trigger**: Se ha corregido el `GameEngine` para que la pasiva se active tanto con disparos normales como con habilidades activas (antes solo funcionaba con disparos normales).
- **Lógica**: La habilidad ahora busca una celda de barco adyacente al hundido y, si no encuentra ninguna (debido a la regla de separación de barcos), selecciona una celda aleatoria de cualquier barco restante en la flota enemiga.
- **Feedback**: Se han actualizado los métodos de habilidades (`Rayo de Piedra Bruja`, `Cohete de Muerte`, etc.) para propagar correctamente el mensaje de "barco revelado" al usuario.

---

### ~~Transformación de Lokhir (Yelmo del Kraken → Venganza de Karond Kar)~~
**RESUELTO (2026-05-13).**
- **Yelmo del Kraken**: Ahora escuda completamente el barco más grande (Arca Negra), pero todos los escudos caen al primer impacto recibido en el navío.
- **Venganza**: Se ha implementado un sistema de transformación dinámico. Si el Arca Negra es hundida, Lokhir pierde su habilidad defensiva y activa **Venganza de Karond Kar** (Ofensiva: 5 disparos aleatorios).
- **UI**: Descripciones actualizadas en frontend y backend.


### ~~Ajuste de dimensiones del Tablero (Espacio lateral)~~
**RESUELTO (2026-05-13).**
- Se ha eliminado la restricción de ancho fijo de `800px` del tablero.
- El tablero ahora utiliza `max-width: min(100%, 82vh)`, permitiéndole expandirse fluidamente para completar el espacio disponible en la columna central, manteniendo su proporción cuadrada y adaptándose a la altura de la pantalla.
- Se ha centrado el tablero tanto horizontal como verticalmente mediante `align-items: center` y `align-self: center` en el grid y su contenedor.

---

### ?? Botn de unin no se bloquea en partidas activas
**Sntoma**: En la lista de salas, el botn 'Unirse' permanece activo incluso si la partida ya ha comenzado (estado: JUGANDO), lo que permite enviar solicitudes de unin invlidas.
**Solucin propuesta**: Deshabilitar o cambiar el texto del botn cuando el estado de la sala no sea ESPERANDO.
