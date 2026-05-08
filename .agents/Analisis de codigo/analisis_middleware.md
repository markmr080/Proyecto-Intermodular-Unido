# Análisis de la capa Middleware (Backend)

Este documento contiene un análisis estructurado de las clases del paquete `com.cifpaviles.proyectofinal.CLMM.middleware`. Esta capa actúa como un puente de seguridad y orquestación entre las peticiones de los clientes (Angular) y la lógica subyacente de la API, especialmente centrada en JWT y WebSockets previos a la partida.

## 1. Configuración y Seguridad (`middleware.config.security` & `middleware.config.socket`)

### `SecurityConfig`
Configura las políticas de seguridad de Spring Security.
- `securityFilterChain(...)`: Deshabilita CSRF, habilita CORS, permite el acceso público a las rutas de login (`/api/auth/login`), y obliga a que cualquier otra petición requiera autenticación. Añade `JwtFilter` antes del filtro estándar de Spring.
- **Uso/Referencias**: Inicializado automáticamente por Spring Boot.

### `JwtFilter` (Extiende `OncePerRequestFilter`)
Filtro que intercepta las peticiones HTTP entrantes para validar los tokens JWT.
- `doFilterInternal(...)`: 
  1. Extrae el token de la cabecera `Authorization: Bearer`.
  2. Valida su firma criptográfica.
  3. Valida el `Fingerprint` (huella digital del navegador) comparando el de la petición con el almacenado dentro del propio token para prevenir robo de sesión.
  4. Implementa "Sliding Session": Si el token está a punto de caducar (dentro de un umbral en milisegundos), genera uno nuevo al vuelo y lo inyecta en las cabeceras de respuesta (`Token-Nuevo`).
- **Uso/Referencias**: Referenciado en `SecurityConfig` y ejecutado automáticamente en cada petición HTTP protegida.

### `JwtProvider`
Componente encargado de crear y verificar matemáticamente los JSON Web Tokens usando el algoritmo HMAC SHA.
- `generarToken(...)`: Dos versiones, una que incrusta el `Fingerprint` como un "Claim" extra, y otra para tokens limpios (ej. recuperación de contraseña).
- `validarToken(...)`: Comprueba que la firma sea legítima.
- `validarFingerprint(...)`: Compara el claim interno del token contra el Fingerprint entregado en la cabecera HTTP.
- `getTiempoRestante(...)`: Calcula los milisegundos de vida que le quedan al token.
- **Uso/Referencias**: Inyectado y usado intensivamente en `JwtFilter` y `AuthService`.

### `SocketIOConfig`
Clase de soporte para configurar el servidor de WebSockets (similar a la de la capa API).

---

## 2. Controladores REST (`middleware.controller`)

### `AuthController`
Orquesta el ciclo completo de autenticación y gestión de perfil.
- `login(...)`: Recibe credenciales y Fingerprint. Devuelve el JWT. Requiere cuenta `middleware_admin` para autenticación base en este entorno.
- `registrar(...)`: Delega en el servicio la creación del usuario.
- `validateUser(...)`: Verifica si un usuario general existe y si sus credenciales son correctas (Login general de usuarios).
- `forgotPassword(...) / resetPassword(...)`: Lógica de recuperación de contraseñas enviando un mail con un token.
- `updatePassword(...) / updateNickname(...) / updateProfilePicture(...)`: Endpoints para la gestión de perfil y edición de datos del jugador.
- **Referencias**: Recibe las llamadas HTTP de las vistas de Angular (LoginComponent, RegisterComponent, ProfileComponent, etc.).

### `UsuarioController`
Controlador enfocado a consultas y operaciones básicas del usuario.
- `crearJugador(...)`: Redirige al registro de usuario (similar a `/register`).
- `obtenerJugador(...)`: Placeholder para consultar los datos de un usuario por su ID.
- **Referencias**: Endpoints invocados por peticiones HTTP.

### `GameController`
- `estadoPartida(...)`: Endpoint de prueba (protegido) que devuelve información leída desde los atributos de la petición dejados por el `JwtFilter` (para comprobar si el token fue renovado).

---

## 3. Servicios del Middleware (`middleware.service.impl`)

### `AuthService` (implementa `IAuthService`)
Intermediario que combina la generación de JWT y las consultas en bases de datos a través de `IUsuarioService` (que pertenece a la API).
- `login(...)`: Bloquea los intentos si el usuario no es "middleware_admin". Valida contraseñas con el servicio API y genera el JWT vía `JwtProvider`.
- `registrar(...)`, `validateUser(...)`, `forgotPassword(...)`, etc.: Mapean directamente hacia los métodos homónimos dentro de `IUsuarioService` que se comunican con MySQL.
- **Referencias**: Inyectado en `AuthController`.

### `GameServiceImpl`
- Servicio vacío o base para futura lógica entre middleware y el estado del juego.

---

## 4. WebSockets para Salas de Espera (Lobby) (`middleware.sockets`)

Mientras que `api.controller.GameSocketController` maneja el combate, el `SocketService` del Middleware maneja **el momento previo** (salas de espera, chat, selección de personaje).

### `SocketService`
Escucha eventos Socket.IO para el Lobby.
- `registrarListeners()` (`@PostConstruct`): Registra la conexión/desconexión y limpia "salas fantasma" si el administrador se cae.
- `registrar-usuario`: Asocia el `socketId` con el ID real del usuario en dos mapas `ConcurrentHashMap` para mensajería privada.
- `solicitar-unirse` / `aceptar-solicitud` / `rechazar-solicitud`: Flujo completo donde el jugador B le pide permiso al jugador A (host) para entrar a su sala, y el host responde.
- `cerrar-sala`: Borra la sala.
- `seleccionar-personaje`: Cuando un usuario cambia su selección, se notifica inmediatamente a la otra persona en la sala.
- `iniciar-partida` / `comenzar-juego`: Eventos que cambian el estado y notifican a ambos clientes que es hora de transicionar al tablero de batalla en Angular.
- **Referencias**: Se conecta e intercambia mensajes con el `SocketService` del Front-End en Angular.

### `LobbyManager`
Servicio inyectable que encapsula el almacenamiento de salas de espera.
- `addRoom(...)`, `getRoom(...)`, `removeRoom(...)`: Manipulación de un `ConcurrentHashMap` que guarda las instancias de `LobbyRoom`.
- **Referencias**: Inyectado en `SocketService` (y en `LobbyController` de la capa API).

### `LobbyRoom`
Modelo de datos simple (DTO) para una sala de espera activa. Contiene IDs, Nombres, Avatares de ambos jugadores y el Estado actual de la sala (`ESPERANDO`, `EN_CURSO`).
