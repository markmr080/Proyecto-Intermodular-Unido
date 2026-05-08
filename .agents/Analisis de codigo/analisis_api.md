# Análisis de la capa API (Backend)

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
- `start()`: Registra los eventos del servidor e inicia la escucha. (Referenciado por Spring Boot con `@PostConstruct`).
- `onConnect(...) / onDisconnect(...)`: Registra la actividad de conexión/desconexión de un socket.
- `onJoinRoom(...)`: Asigna a un cliente a una sala determinada. Instancia el estado del juego si no existe, o añade al jugador 2 al estado. Delega a `GameRoomManager` y `CharacterFactory`.
- `onAtacar(...)`: Delega el procesamiento de un disparo en `GameEngine.procesarDisparo()`. Si la partida termina, guarda todo usando `finalizarPartidaBD()`. (Escuchado desde el frontend `atacar`).
- `onUsarHabilidad(...)`: Delega la ejecución a `GameEngine.usarHabilidad()`.
- `onColocarBarcos(...)`: Marca que un jugador colocó su flota. Cuando los dos jugadores están listos, inicia la fase `COMBATE` llamando a `iniciarPartidaBD()`.
- `iniciarPartidaBD(GameState state)`: Crea una `PartidaEntity` en estado `EN_CURSO` en MySQL a través de `PartidaRepository`. (Método interno).
- `finalizarPartidaBD(GameState state)`: Cierra la `PartidaEntity` guardando al ganador en MySQL y llama a `guardarStatsJugador()` para los datos de Mongo. (Método interno).
- `guardarStatsJugador(...)`: Delega a `EstadisticasService` el almacenamiento de hits/fallos en MongoDB. (Método interno).
- `difundirEstado(...)`: Notifica a los usuarios en la sala los cambios de estado. (Método interno).

### `LobbyController`
Controlador REST para las salas de espera.
- `getLobbyRooms()`, `createLobbyRoom(...)`, `deleteLobbyRoom(...)`: Métodos que interactúan directamente con la memoria volatil del `LobbyManager` (perteneciente a `middleware.sockets`).
- **Referencias**: Endpoints invocados por peticiones HTTP del frontend (e.g. Angular).

### `PartidaController`
Controlador REST para revisar historial de partidas.
- `listarPartidas()`, `listarPorEstado(...)`, `obtenerPartida(...)`, `eliminarPartida(...)`: Acceden a la base de datos MySQL vía `PartidaRepository` para consultar/borrar partidas históricas.
- **Referencias**: Invocados por el frontend (rutas de administrador o historial).

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
Gestiona un HashMap en memoria concurrente con los GameEngine activos.
- `getOrCreateRoom(...)`: Obtiene la sala de la memoria, si no está la instancia vacía. (Referenciado en `GameSocketController`).
- `getRoom(...)`: Devuelve la sala existente o null.
- `removeRoom(...)`: Purga la sala terminada de la memoria RAM.

### `CharacterFactory`
Fábrica inyectable que construye las Clases Base de los Personajes.
- `crearPersonaje(String tipo)`: Instancia la clase según el nombre (Artillero, Comandante), accede a `PersonajeRepository` y `PersonajeFlotaRepository` para leer los barcos que tiene el jugador, y devuelve un objeto `GameCharacter`. (Referenciado en `GameSocketController.onJoinRoom`).

### `TurnTimerService`
Servicio para controlar cronómetros en hilos background.
- `iniciarCronometro()`: Inicia un hilo con `ScheduledExecutorService` restando un segundo cada iteración.
- `actualizarSegundo()`, `manejarTiempoAgotado()`: Bajan el tiempo y ejecutan la acción de saltar el turno si se llega a 0.
- **Referencias**: En proceso de integración. No utilizado explícitamente en el flujo de `GameSocketController` de momento.

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
