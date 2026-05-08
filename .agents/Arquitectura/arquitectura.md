# PROYECTO INTERMODULAR 2º DAM
## Warhammer Battleship - Arquitectura y Especificaciones

### Contenido
1. INFORMACIÓN GENERAL DEL PROYECTO
2. MODELO DE SEGURIDAD
3. ACTORES DEL SISTEMA
4. MÓDULOS DEL SISTEMA
5. REQUISITOS FUNCIONALES DETALLADOS
6. MODELO DE DATOS
7. RESUMEN DE FLUJOS

---

## 1. INFORMACIÓN GENERAL DEL PROYECTO
**Nombre:** Warhammer Battleship (Hundir la flota)
**Tipo:** Aplicación Web Multiplataforma (Cliente-Servidor en Tiempo Real)
**Tecnologías principales:**
- **Frontend:** Angular 17+ (Standalone Components), HTML5, CSS3, Socket.IO Client.
- **Backend:** Spring Boot (Java 20+).
- **Bases de Datos:** MySQL (JPA para lógica transaccional) y MongoDB Atlas (para almacenamiento de estadísticas y partidas).
- **Comunicación en Tiempo Real:** Netty-SocketIO (para Lobby y Tablero).

**Arquitectura General:**
El backend se estructura como un **Monolito Modular** separado lógicamente en dos grandes bloques:
- **Middleware (`middleware`):** Gestiona la seguridad, autenticación, y las salas de espera previas a las partidas. Intercepta todas las peticiones para proteger a la base de datos subyacente.
- **API (`api`):** Gestiona la conexión a las bases de datos (MySQL/MongoDB), el historial de los usuarios y las reglas/motor del juego durante las partidas reales.

---

## 2. MODELO DE SEGURIDAD

### 2.1. Arquitectura de Autenticación
La seguridad se basa en una arquitectura **"Zero Trust"** hacia el cliente y un mecanismo de **autenticación en dos fases** (Aislamiento Perimetral):
```text
Usuario Final (Frontend) → Middleware (Spring Security JWT) → API / BBDD
```
1. **Fase 1 (Aislamiento):** Las llamadas de autenticación del frontend obtienen un token inicial temporal usando una cuenta interna `middleware_admin`.
2. **Fase 2 (Login Real):** Una vez dentro del perímetro seguro, se contrastan las credenciales reales del usuario contra la base de datos, y se le entrega su JWT de sesión definitivo.

### 2.2. Principios de Seguridad
| Principio | Descripción |
| :--- | :--- |
| **Fingerprinting** | El JWT incluye un *claim* con el hash (SHA-256) del navegador del usuario. El Frontend inyecta la cabecera HTTP `X-Fingerprint`. Si el Fingerprint del JWT no coincide con la cabecera, se deniega el acceso para evitar robos de sesión. |
| **Sliding Session** | El `JwtFilter` detecta si el token está próximo a caducar. Si es así, inyecta un nuevo token en la cabecera de respuesta `Token-Nuevo` y el Frontend lo actualiza automáticamente. |
| **Stateless** | No hay persistencia de sesión en disco ni caché de sesión (ej: Redis). La validez depende puramente de la firma criptográfica del JWT y el tiempo de expiración. |

---

## 3. ACTORES DEL SISTEMA
| Actor | Descripción |
| :--- | :--- |
| **Jugador (Guest)** | Usuario no autenticado que interactúa con la pantalla de Login y Registro. |
| **Jugador (Autenticado)** | Usuario que navega por el menú, lista de salas, actualiza su perfil, y puede unirse o crear una partida. |
| **Administrador Interno** | Cuenta de servicio (`middleware_admin`) que permite al frontend establecer el primer túnel de comunicación con el backend para los procesos de inicio de sesión y recuperación de contraseñas. |

---

## 4. MÓDULOS DEL SISTEMA

### 4.1 Módulo Middleware (Seguridad y Lobby)
- Filtros de JWT (`JwtFilter`, `JwtProvider`).
- Gestión de Usuarios y Perfiles en tiempo real (`AuthController`, `UsuarioController`).
- Salas de Espera (`LobbyController` y `SocketService` con gestión in-memory a través de `LobbyManager`).

### 4.2 Módulo API (Transaccional)
- Entidades relacionales (Usuarios, Partidas, Catálogo de Barcos, Personajes).
- Servicios de persistencia.

### 4.3 Módulo GameEngine (Motor de Juego)
- Controla el estado del tablero, valida los disparos contra la matriz, comprueba hundimientos y decide el fin de la partida.
- Contiene los modificadores aplicados por las Habilidades de los Personajes (`CharacterFactory`).

### 4.4 Módulo de Estadísticas (MongoDB)
- Almacena el desglose granulado de las partidas (`PartidaStatsDocument`): impactos acertados, fallos, muertes, victorias, para que sean procesados analíticamente sin sobrecargar MySQL.

---

## 5. REQUISITOS FUNCIONALES DETALLADOS

**RF-01: Registro de Usuarios**
- El jugador debe poder crear una cuenta introduciendo username, password y email (validado). La contraseña se cifra con BCrypt.

**RF-02: Autenticación y Cierre de Sesión**
- Los jugadores inician sesión para obtener un JWT almacenado en memoria volátil (`sessionStorage`). Al cerrar sesión, el token se borra del cliente.

**RF-03: Recuperación de Contraseña**
- Sistema por email que permite enviar un enlace de un solo uso con un token cifrado para resetear la contraseña del jugador en caso de olvido.

**RF-04: Gestión del Perfil**
- Un usuario puede cambiar su "nickname", su "password" o el enlace de su "avatar" (procedural por Dicebear).

**RF-05: Listado y Creación de Salas (Lobby REST)**
- Los jugadores pueden obtener una lista de las partidas que están en estado `ESPERANDO`.
- Pueden crear una nueva sala con un código generado aleatoriamente.

**RF-06: Gestión del Lobby (WebSockets)**
- Un jugador B puede enviar una "solicitud para unirse" a una sala. El jugador A (creador) recibe la notificación y decide "aceptar" o "rechazar".

**RF-07: Selección de Personaje**
- En una sala consolidada (2/2 jugadores), los usuarios eligen una de las 5 facciones/capitanes (Wulfrik, Ayslinn, Lokhir, Aranessa...). Esto determina las estadísticas y habilidades de su flota.

**RF-08: Combate (Tablero en tiempo real)**
- Los jugadores posicionan en una cuadrícula (10x10) un total de 5 barcos.
- El juego se rige por un sistema de turnos sincronizado por el backend.
- Los jugadores pueden realizar ataques simples o utilizar habilidades (ofensivas y defensivas) con *cooldown*.

**RF-09: Estadísticas y Ranking**
- Al finalizar una partida, el estado global se guarda en MySQL, y los resultados detallados de rendimiento (precisión, impactos, barcos hundidos) se indexan en MongoDB.
- El jugador puede ver un resumen numérico de su desempeño global desde su panel principal.

---

## 6. MODELO DE DATOS

La persistencia de este proyecto aplica el patrón de base de datos híbrida (Polyglot Persistence):

### 6.1 MySQL (Relacional / Core)
Se encarga de la lógica ACID y persistencia obligatoria:
- **USUARIOS:** Identificador, Credenciales seguras (BCrypt), Email.
- **PARTIDAS:** Identificador, Host de la partida, Ganador de la partida, Fechas de inicio/fin, Estado final.
- **PERSONAJES:** Catálogo de facciones elegibles.
- **BARCOS_CATALOGO y PERSONAJE_FLOTA:** Configuración de la estructura de las naves asignadas a cada personaje.

### 6.2 MongoDB Atlas (Documental / Analytics)
Se encarga de las lecturas analíticas masivas, sin necesidad de un esquema restrictivo:
- **PARTIDA_STATS:** Colección donde por cada partida jugada y jugador involucrado, se guarda un JSON con: idUsuario, idPartida, personaje utilizado, impactos acertados, impactos fallados y barcos hundidos.

---

## 7. RESUMEN DE FLUJOS TÍPICOS

### Flujo de Partida Completa:
1. Jugador se loguea en Angular (HTTP).
2. Entra a "Crear Partida" -> Llama a `LobbyController` (HTTP) -> Se genera `LobbyRoom` in-memory.
3. Se conecta al Socket.IO (8081).
4. Otro jugador hace click en "Unirse" -> Envía `solicitar-unirse` (WS).
5. Host acepta -> `solicitud-aceptada` (WS) -> Ambos navegan a `/seleccion-personajes`.
6. Seleccionan personaje (Wulfrik vs Ayslinn) -> Click en "Empezar" -> `comenzar-juego` (WS).
7. Ambos navegan a `/partida-activa`. El servidor inicializa en su memoria el `GameEngine`.
8. Se turnan disparando (`atacar`) o usando pasivas. El servidor responde con `gameState`.
9. Cuando uno se queda sin barcos, el `GameEngine` cierra la partida, emite el fin (WS) e invoca de manera asíncrona a los servicios de persistencia de JPA y MongoDB.