# Análisis de la capa API (Backend)

> **Actualizado:** 2026-05-12

Este documento contiene un análisis estructurado de las clases del paquete `com.cifpaviles.proyectofinal.CLMM.api`, explicando la función de cada método y en qué lugar del sistema está referenciado o utilizado.

## 1. Configuración (`api.config`)

### `JpaConfig`
Configura de forma dedicada el escaneo de repositorios JPA para MySQL en paquetes específicos.
- **Uso/Referencias**: Inicializado automáticamente por Spring Boot durante el arranque de la aplicación.

### `MongoConfig`
Configura el escaneo de los repositorios de MongoDB en el paquete específico `mongo`.
- **Uso/Referencias**: Inicializado automáticamente por Spring Boot para evitar conflictos con JPA.

### `PasswordConfig`
- `passwordEncoder()`: Devuelve una instancia de `BCryptPasswordEncoder` como bean.
- **Uso/Referencias**: Inyectado por Spring en `UsuarioService` para cifrar y comprobar contraseñas.

### `SocketIOConfig`
- `socketIOServer()`: Levanta el servidor de WebSockets en un puerto determinado (por defecto 8081) y permite CORS para el cliente Angular.
- **Uso/Referencias**: Su bean de retorno (`SocketIOServer`) es inyectado en `GameSocketController`.

---

## 2. Controladores (`api.controller`)

### `GameSocketController`
Maneja toda la interacción de juego en tiempo real (WebSockets).
- `start()`: Registra los eventos del servidor e inicia la escucha (`@PostConstruct`).
- `onConnect(...) / onDisconnect(...)`: Registra la actividad de conexión/desconexión de un socket.
- `onJoinRoom(...)`: Asigna a un cliente a una sala. Crea estado nuevo si la sala no existe o si la partida anterior ya finalizó. El segundo jugador reemplaza al `enemigo-dummy`. Delega a `GameRoomManager` y `CharacterFactory`.
- `onAtacar(...)`: Delega el procesamiento en `GameEngine.procesarDisparo()`. Llama a `limpiarSalaFinalizada()` para guardar y limpiar si termina. (Evento `atacar`).
- `onUsarHabilidad(...)`: Delega en `GameEngine.usarHabilidad()` con coordenadas `x,y`. Llama también a `limpiarSalaFinalizada()`. (Evento `usar-habilidad`).
- `onColocarBarcos(...)`: Cuando ambos jugadores colocan su flota, pasa a fase `COMBATE`, llama a `iniciarPartidaBD()` e inicia el timer compartido con `roomManager.startTimer(roomCode)`. (Evento `colocar-barcos`).
- `onRendirse(...)`: ✅ **NUEVO**. Declara ganador al rival del jugador que se rinde. Difunde el estado final y llama a `limpiarSalaFinalizada()`. (Evento `rendirse`).
- `iniciarPartidaBD(GameState state)`: Crea una `PartidaEntity` en estado `EN_CURSO` en MySQL. Guarda el ID de MySQL en el `GameState` para enlazarla al finalizar. (Método interno).
- `finalizarPartidaBD(GameState state)`: Cierra la `PartidaEntity` con ganador y `fechaFin` en MySQL. Llama a `guardarStatsJugador()` para ambos jugadores en MongoDB. (Método interno).
- `guardarStatsJugador(...)`: Delega en `EstadisticasService.guardarStatsPartida()` para persistir hits/fallos/barcos hundidos en MongoDB. (Método interno).
- `limpiarSalaFinalizada(...)`: ✅ **NUEVO**. Si el juego terminó y no se han guardado stats aún, llama a `finalizarPartidaBD()`. Luego programa la eliminación de la sala con 10 segundos de retardo (hilo `CompletableFuture`) para que ambos clientes reciban el estado final. (Método interno).
- `difundirEstado(...)`: Emite el evento `gameState` a todos los clientes de la sala. (Método interno).

### `EstadisticasController` ✅ CREADO
Controlador REST para exponer estadísticas de jugadores.
- `getEstadisticasJugador(username)`: `GET /api/estadisticas/jugador/{username}`. Delega en `IEstadisticasService.getStatsAgregadas()` y devuelve un `StatsAgregadasDTO` con datos cruzados de MySQL y MongoDB.
- **Referencias**: Llamado desde `AuthService.getUserStats()` en el frontend Angular.

### `LobbyController`
Controlador REST para las salas de espera.
- `getLobbyRooms()`, `createLobbyRoom(...)`, `deleteLobbyRoom(...)`: Interactúan con el `LobbyManager` en memoria del paquete `middleware.sockets`.
- **Referencias**: Endpoints invocados por `RoomService` del frontend Angular.

### `PartidaController`
Controlador REST para revisar historial de partidas.
- `listarPartidas()`, `listarPorEstado(...)`, `obtenerPartida(...)`, `eliminarPartida(...)`: Acceden a `PartidaRepository` (MySQL) para consultar/borrar partidas históricas.
- **Referencias**: Invocados por el frontend (historial o panel de administración).

### `PersonajeController`
Controlador REST para obtener el catálogo de personajes disponibles.
- **Referencias**: Usado por el frontend en la vista de selección de personajes.

---

## 3. Servicios de Juego (`api.service.game`)

### `GameEngine`
Mantiene la lógica pura de la batalla para una sala específica.
- `procesarDisparo(...)`: Gestiona las validaciones de turno, colisiones de disparo con tableros, actualización de `CellStatus`, acumulación de puntos de hit/fallo y cambio de fases. (Referenciado por `GameSocketController.onAtacar`).
- `activarFaseReaccion()`: Activa el límite de tiempo de 20 segundos de reacción. (Método interno).
- `usarHabilidad(...)`: Gestiona el cooldown de las habilidades, delega la ejecución de habilidades. (Referenciado por `GameSocketController.onUsarHabilidad`).
- `ejecutarEfectoHabilidad(...)`: Aplicaría la mutación real sobre la partida según el tipo de habilidad (en desarrollo).
- `verificarVictoria()`: Revisa la vida (estado vivo/muerto) y delega a `finalizarJuego()` si hay un perdedor. (Método interno).
- `finalizarJuego(...)`: Determina el ganador y marca el juego como inactivo. (Método interno).

### `GameRoomManager`
Gestiona los GameEngine y TurnTimerService activos en memoria (dos `ConcurrentHashMap` separados).
- `getOrCreateRoom(roomCode)`: Devuelve el `GameEngine` existente o crea uno nuevo vacío. (Usado en `GameSocketController.onJoinRoom`).
- `getRoom(roomCode)`: Devuelve el `GameEngine` existente o `null`. (Usado en `onAtacar`, `onUsarHabilidad`, etc.).
- `startTimer(roomCode)`: ✅ **NUEVO**. Crea un `TurnTimerService` para la sala y lo inicia. Solo puede haber un timer por sala. Se llama al pasar a fase `COMBATE`. (Usado en `onColocarBarcos`).
- `removeRoom(roomCode)`: Elimina la sala del mapa y detiene su timer (`timer.detener()`). Llamado por `limpiarSalaFinalizada()` tras 10 segundos.

### `CharacterFactory`
Fábrica inyectable que construye objetos `GameCharacter` con sus habilidades y flota.
- `crearPersonaje(String tipo)`: Instancia el personaje según su ID (`WULFRIK`, `AISLINN`, `LOKHIR`, `ARANESSA`), carga sus barcos desde `PersonajeRepository` y `PersonajeFlotaRepository`. Devuelve un `GameCharacter` con sus habilidades activas y pasiva configuradas. (Usado en `GameSocketController.onJoinRoom`).

### `TurnTimerService`
✅ **TOTALMENTE INTEGRADO**. Cronómetro dedicado por sala de juego.
- `iniciarCronometro()`: Lanza un `ScheduledExecutorService` de un solo hilo que se ejecuta cada segundo.
- `actualizarSegundo()`: Decrementa `tiempoRestante` en el `GameState`. Si llega a 0, llama a `manejarTiempoAgotado()`.
- `manejarTiempoAgotado()`: Llama a `state.cambiarTurno()`, reiniciando el timer a 60s para el jugador activo.
- `difundirEstado()`: Emite el evento `gameState` a todos los clientes de la sala en cada tick.
- `detener()`: Llama a `scheduler.shutdown()` para liberar el hilo. Es invocado por `GameRoomManager.removeRoom()`.
- **Instanciación**: Creado por `GameRoomManager.startTimer()`, que es llamado desde `GameSocketController.onColocarBarcos()` cuando ambos jugadores están listos.

---

## 4. Servicios de Implementación (`api.service.impl`)

### `UsuarioService` (implementa `IUsuarioService`)
- `crearUsuarioSistema()`: Anotado con `@PostConstruct`. Crea de forma obligatoria el usuario `middleware_admin`.
- `registrarUsuario(...)`: Guarda el usuario con bcrypt y valida redundancias. Llama a `EmailService`. (Referenciado desde controladores Middleware).
- `validarCredenciales(...)`: Comprueba la constraseña usando bcrypt. Devuelve la entidad de usuario o tira error.
- `actualizarPassword(...)`, `enviarCorreoRecuperacion(...)`, `actualizarUsername(...)`: Modifican base de datos MySQL (Referenciados desde endpoints).

### `EstadisticasService` (implementa `IEstadisticasService`)
- `getStatsAgregadas(...)`: Suma todas las variables de los documentos MongoDB de un usuario, y además cruza la información para contar victorias desde el MySQL `PartidaRepository`. Devuelve `StatsAgregadasDTO`. (Referenciado por controladores del Middleware).
- `guardarStatsPartida(...)`: Salva un `PartidaStatsDocument` en MongoDB (Referenciado por `GameSocketController`).
- `getHistorial(...)`: Recupera lista de MongoDB directamente.

### `EmailService` (implementa `IEmailService`)
- `enviarCorreoBienvenida(...)`: Envía mensaje plano asíncrono. (Referenciado por `UsuarioService`).
- `enviarCorreoRecuperacion(...)`: Compone un mensaje `MimeMessage` (HTML) con un enlace temporal. 

---

## 5. Repositorios de Bases de Datos (`api.repository`)

- **`UsuarioRepository` (MySQL)**: Utilizado por `UsuarioService` y `EstadisticasService` para consultas por username/email.
- **`PartidaRepository` (MySQL)**: Utilizado en `GameSocketController` para crearla y cerrarla, en `PartidaController` para exponerlas a la web y en `EstadisticasService` para contar partidas ganadas.
- **`EstadisticasRepository` (MongoDB)**: Utilizado en `EstadisticasService` para almacenar hits, fallos y barcos hundidos en formato BSON.
- **`PersonajeRepository` / `PersonajeFlotaRepository` (MySQL)**: Consultados en la `CharacterFactory` para pre-poblar la lista de barcos que posee el GameCharacter según la base de datos relacional.
