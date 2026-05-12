# Análisis Detallado del Frontend (Angular)

> **Actualizado:** 2026-05-12

Este documento detalla la estructura y componentes principales del proyecto cliente en Angular (`front-conectado-main`), centrándose en explicar pormenorizadamente los controladores `.ts` y las referencias entre las plantillas `.html` (botones, formularios) y la lógica subyacente.

## 1. Servicios Base (`src/app/services`)

### `AuthService`
Es el núcleo de la seguridad y comunicación HTTP del frontend con el middleware.
- **Resolución dinámica de host**: Las URLs base `API_URL` y `STATS_URL` se construyen comprobando `window.location.hostname`. Si es `localhost` usa `http://localhost:8080`; en producción usa la misma IP/dominio de la página.
- **Fingerprinting**: Genera un hash SHA-256 en memoria (`generarFingerprint()`). En contextos HTTP no seguros (sin HTTPS), usa un fallback simple de hash de 32 bits para mantener compatibilidad.
- **Aislamiento de la BD**: Todas sus llamadas (login, register, update) pasan primero por `withMiddlewareToken()` para obtener el token de `middleware_admin` y así entrar a la zona segura.
- **Persistencia**: `saveToken()`, `logout()` y lectura guardan los datos en `sessionStorage` (volátil por pestaña). El fingerprint solo vive en memoria (variable privada `cachedFingerprint`).
- **Estadísticas**: `getUserStats(username)` llama a `GET /api/estadisticas/jugador/{username}` con token de middleware y fingerprint.

### `SocketService`
Gestiona la conexión WebSocket (Puerto 8081).
- **Resolución dinámica de host**: La URL de conexión se construye en `connect()` comprobando `window.location.hostname`. En `localhost` usa `http://localhost:8081`; en producción usa `https://{hostname}` para pasar por el proxy Nginx.
- **Lobby y Tablero**: Expone métodos imperativos para enviar eventos (`solicitarUnirse`, `atacar`, `usarHabilidad(id, x, y)`) y `Subject` reactivos para que los componentes se suscriban.
- **Evento `rendirse`**: Método `rendirse(jugadorId, roomCode)` que emite el evento `rendirse` al backend para declarar al rival ganador.
- **Habilidades con coordenadas**: `usarHabilidad(id, x, y)` admite coordenadas de celda para habilidades de área (`x=-1, y=-1` para las que no necesitan objetivo).
- **Limpieza**: `disconnect()` cierra el socket correctamente.

### `RoomService`
Servicio para llamadas HTTP clásicas (`GET`, `POST`, `DELETE`) al endpoint `/api/lobby` para gestionar las salas de espera pre-partida.

## 2. Interceptores (`src/app/interceptors`)

### `AuthInterceptor` (`auth.interceptor.ts`)
Filtro HTTP de Angular.
- **A la ida**: Inyecta automáticamente el Token JWT y el Fingerprint en la cabecera de todas las peticiones a la API.
- **A la vuelta**: Implementa el mecanismo de "Sliding Session" escuchando la cabecera `Token-Nuevo`. Si el Backend detecta que el token original caducará pronto, envía uno nuevo en la respuesta, y el interceptor lo almacena silenciosamente prolongando la sesión.

---

## 3. Análisis Detallado de Componentes (.ts y .html)

A continuación, se detalla qué hace exactamente cada archivo `.ts` y cómo los botones de sus plantillas `.html` los mandan llamar.

### 3.1 Componente: Login (`login`)

**Lógica (`login.component.ts`):**
- Declara el formulario reactivo `loginForm` con `username` y `password`.
- **`onSubmit()`**: Extrae las credenciales y llama al método reactivo `AuthService.login()`. Modifica el HTML enseñando errores concretos provenientes de la API (e.g., "PASSWORD_INCORRECTO"). Si va bien, navega a `/menu`.
- **`togglePasswordVisibility()`**: Alterna la bandera `showPassword` para ocultar o desocultar los asteriscos del input.
- **Recuperación**: Dispone de variables y funciones para abrir/cerrar un modal, y de `sendRecoveryEmail()` que manda el correo a través del servicio.

**Referencias de Botones (`login.component.html`):**
- **Botón "Mostrar/Ocultar contraseña" (Icono ojo)**: Posee el evento `(click)="togglePasswordVisibility()"`.
- **Enlace "Olvidé la contraseña"**: Posee el evento `(click)="openRecoveryModal($event)"`.
- **Botón "Entrar" (`type="submit"`)**: Al clicar o pulsar Enter en un input, lanza el evento implícito `(ngSubmit)="onSubmit()"` declarado en el tag `<form>`.
- **Botón "×" del modal y Área oscura de fondo**: Disparan `(click)="closeRecoveryModal()"`.
- **Botón "Enviar Enlace" (Dentro del modal)**: Llama explícitamente a `(click)="sendRecoveryEmail()"`.

### 3.2 Componente: Menú Principal (`menu`)

**Lógica (`menu.component.ts`):**
- Escucha el usuario logueado en tiempo real con `authService.user$.subscribe()`.
- Contiene una lista estática de previsualización de `characters` y `availableAvatars`.
- **`nextChar()`, `prevChar()`**: Actualizan el índice numérico `currentMobileIndex` que resalta uno u otro personaje en vistas estrechas.
- **`logout()`**: Borra los tokens y devuelve al usuario a la ruta `/login`.
- **`abrirModalPerfil()` y `abrirModalJugar()`**: Llaman al Router de Angular para redirigir a `/perfil` y `/lista-salas`.

**Referencias de Botones (`menu.component.html`):**
- **Botón enorme "JUGAR"**: Posee el evento `(click)="abrirModalJugar()"`.
- **Área con Foto y Nombre (Arriba a la derecha)**: Posee el evento `(click)="abrirModalPerfil()"`.
- **Botón "Salir"**: Enlazado a `(click)="logout()"`.
- **Botones "<" y ">" (Carrusel)**: Llevan un evento `(click)="prevChar()"` y `(click)="nextChar()"` respectivamente.

### 3.3 Componente: Lista de Salas (`lista-salas`)

**Lógica (`lista-salas.ts`):**
- En el `ngOnInit`, usa `setInterval` para recargar pasivamente todas las salas (`roomService.getRooms()`) cada 10 segundos, mostrando solo las que tienen estado "ESPERANDO".
- Se suscribe al canal WebSockets y espera si el rival responde a la solicitud con `solicitudAceptada$` o `solicitudRechazada$`. Si te aceptan, navega a la vista de Lobby.
- **`crearSala()`**: Forma un JSON con datos de tu avatar y nombre, lo manda al API, y usa el Router para meterte automáticamente a tu sala vacía recién creada.
- **`unirse(room)`**: Toma la ID de la sala y manda un mensaje WebSocket (`solicitarUnirse`) indicándole al anfitrión de esa sala que tú quieres entrar.

**Referencias de Botones (`lista-salas.html`):**
- **Botón de recarga manual (Icono circular)**: Posee `(click)="recargarSalas()"`.
- **Botón "CREAR SALA"**: Referenciado a `(click)="crearSala()"`.
- **Botón "Unirse"**: Renderizado dinámicamente (`@for`). Envía el objeto de la sala particular a `(click)="unirse(room)"`. Si la sala está llena se oculta y aparece un botón deshabilitado que dice "Sala llena".
- **Botón "Salir"**: Referenciado a `(click)="salir()"`. Vuelve al menú base.

### 3.4 Componente: Selección de Personajes (`seleccion-personajes`)

**Lógica (`seleccion-personajes.component.ts`):**
- **Carga de Sala**: Evalúa si el usuario logueado es el dueño de la sala ("Jugador 1") o el invitado ("Jugador 2"). 
- **`anterior()`, `siguiente()`**: Cambian la variable `indiceActual` que muta el personaje visualizado (del 1 al 5).
- **`seleccionar()`**: Cuando el jugador confirma, guarda su elección a nivel local (`seleccionJugador1` o `2`) y notifica al contrincante a través del socket con `socketService.seleccionarPersonaje()`.
- **`empezarPartida()`**: Es una función a la que solo accede el Jugador 1 (Anfitrión) cuando detecta que ambos tienen el estado "listo". Manda el evento `comenzarJuego` al backend.
- Escucha activamente a `SocketService.juegoComenzado$`; cuando el backend da la luz verde, **el router redirige automáticamente a ambos clientes** al tablero final (`/partida-activa/:code`).

**Referencias de Botones (`seleccion-personajes.component.html`):**
- **Botón Flecha Izquierda / Derecha**: Mapeados a `(click)="anterior()"` y `(click)="siguiente()"`.
- **Botón "Seleccionar" / "Personaje elegido"**: Ligado a `(click)="seleccionar()"`.
- **Botón "⚔ Empieza la partida"**: Solo se muestra en el DOM si el `jugadorActual` es 1. Está deshabilitado mediante `[disabled]="!ambosProntos"`. Si ambos jugadores están listos, permite hacer `(click)="empezarPartida()"`.

### 3.5 Componente: Tablero / Partida Activa (`partida-activa`)

**Lógica (`partida-activa.component.ts`):**
- Representa la culminación en tiempo real. En el arranque conecta el WebSocket (`socketService.connect()`) y entra explícitamente a la sala técnica de la batalla (`joinRoom`).
- Se suscribe a `gameState$`. Cuando el backend manda una actualización JSON, la guarda, evalúa de quién es el turno (`esMiTurno`) y extrae los tableros modificados al modelo visual.
- **Fase Colocación**: Lógica pesada local. Almacena las siluetas de los barcos (`colocarBarcoEnCelda()`), previene que un barco se ponga encima de otro o fuera de los límites (usando la variable booleana `orientation: H/V`), y cuando están todos (`currentShipIndex == 5`), permite mandar la matriz local hacia el backend mediante `enviarTablero()`.
- **Fase Combate**: Si la variable de estado es "COMBATE", se habilitan los clickers sobre el tablero enemigo (`atacarCasilla(x,y)`) que mandan por socket las coordenadas exactas X e Y. Las habilidades (`usarHabilidad(id)`) también envian las coordenadas `x,y` seleccionadas para habilidades de área.
- **Condición de ataque corregida**: La guarda de ataque es `if (gameState?.fase !== 'COMBATE' || !esMiTurno) return;` (ya no usa `faseReaccion` que bloqueaba erroneamente a J2).

**Referencias de Botones y Áreas Clicables (`partida-activa.component.html`):**
- **Botón "Orientación: Horizontal / Vertical"**: Usado en Fase Colocación con `(click)="cambiarOrientacion()"`.
- **Botón "LISTO"**: Oculto hasta que se terminen de colocar los 5 barcos. Apunta a `(click)="enviarTablero()"`.
- **Casillas DIV propias (`.cell.mi-tablero`)**: Cada cuadradito de agua invoca `(click)="colocarBarcoEnCelda(x, y)"` si estamos en la fase de colocación.
- **Casillas DIV enemigas (`.cell.enemigo-tablero`)**: Cada cuadradito del mar enemigo invoca `(click)="atacarCasilla(x, y)"`. La lógica del TS y el Backend ignorarán el click si el turno no es tuyo.
- **Botones circulares de habilidad (`.ability-circle`)**: Dispuestos a ambos lados del tablero en columnas de ofensivo y defensivo, apuntan a `(click)="usarHabilidad(hab.id)"`. Tienen la propiedad de Angular `[disabled]` atada a los cooldowns numéricos (se quedan en gris si ya los usaste, si no es tu turno o si la penalización aún dura).
