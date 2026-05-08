# Agent Context: Warhammer Battleship (Hundir la Flota)

## 1. Perfil del Proyecto
- **Nombre:** Warhammer Battleship.
- **Tipo:** Proyecto Intermodular 2º DAM.
- **Descripción:** Juego de "Hundir la Flota" con temática de Warhammer, arquitectura de microservicios simulada (Middleware y API) y sistema de persistencia híbrido.

## 2. Stack Tecnológico
- **Backend:** Java 21 / Spring Boot.
- **Frontend:** Angular 19 (Localizado en la carpeta `front-conectado-main`).
- **Comunicaciones:** REST (Spring Web) y WebSockets (Netty-SocketIO).
- **Despliegue/Infraestructura (Producción):** Servidor Ubuntu en Azure con contenedores gestionados mediante **Docker Compose**.
- **Base de Datos Relacional (Core):** MySQL 8 (Contenerizado).
- **Base de Datos Documental (Analítica):** MongoDB oficial (Contenerizado). Se descarta el uso de MongoDB Atlas.

## 3. Arquitectura de Seguridad (JWT Personalizado)
- **Aislamiento Perimetral:** `middleware_admin`. Es una cuenta interna del backend utilizada en la "Fase 1" del login para otorgar acceso al perímetro seguro antes de validar las credenciales del usuario final.
- **Almacenamiento:** El token JWT se guarda en el **`sessionStorage`** del navegador (se borra al cerrar la pestaña, más seguro que `localStorage`).
- **Protección Fingerprint:** El token contiene un hash (SHA-256) generado a partir de las propiedades del navegador. Todas las peticiones protegidas exigen tanto el token (`Authorization: Bearer`) como la huella (`X-Fingerprint`).
- **Sistema de Sliding Session:** 
    - Duración del token base: **1 hora**.
    - Renovación: Si se hace una petición y al token le faltan **10 minutos o menos**, el backend devuelve un JWT fresco en la cabecera HTTP `Token-Nuevo`, prolongando la sesión transparentemente.

## 4. Módulos y Lógica de Negocio
### A. Gestión de Usuarios y Autenticación
- **Registro:** Usa los campos `username`, `email` y `password` (cifrado con BCrypt). *(No existen los campos nombre y apellidos)*.
- **Flujo Contraseña:** Sistema robusto de recuperación enviando un enlace de un solo uso por correo mediante **SMTP Gmail**.
- **Avatares:** Se generan procedimentalmente o el usuario indica su propia URL.

### B. El Juego (Hundir la Flota)
- **Lobby en Memoria:** Los jugadores buscan y crean salas mediante HTTP (`/api/lobby`) que se gestionan en memoria hasta que hay 2 jugadores.
- **Mecánica Sockets:** Una vez arranca la fase de selección de personajes o colocación, toda la interacción (`solicitar-unirse`, `atacar`, `usar-habilidad`) viaja a través de WebSockets (Socket.IO). El backend (`GameEngine`) valida las jugadas e informa a ambos clientes (`gameState`).
- **Persistencia Híbrida Diferida:** La partida no guarda cada disparo en la base de datos. Solo **al finalizar la partida**, el servidor guarda:
    1. Quién es el ganador y la duración en `PARTIDAS` (MySQL).
    2. Las métricas exactas (impactos acertados, fallados, barcos hundidos) en `PARTIDA_STATS` (MongoDB), creando un documento para cada jugador.

## 5. Reglas de Desarrollo, Seguridad y Configuración
- **Sanitización OWASP:** El código bloquea XSS con validación y (próximamente) la librería `owasp-java-html-sanitizer`. Se valida la sintaxis de las entradas mediante `@Valid` (Jakarta Validation) incluyendo seguridad en URLs (`@URL`). SQL Injection prevenido usando `PreparedStatement` de Spring Data JPA.
- **Configuración properties:** Las credenciales de base de datos y la clave `jwt.secret` (que requiere min 256 bits) deberán pasarse como variables de entorno en producción.
- **JPA DDL:** En desarrollo Hibernate está en `update`, pero se modificará a `validate` o `none` para producción.

## 6. Endpoints y Eventos Principales
- **Auth REST (`/api/auth`)**: `/login` (Túnel), `/validate-user` (Login Real), `/register`, `/forgot-password`, `/update-nickname`.
- **Lobby REST (`/api/lobby`)**: Creación, listado y borrado in-memory de salas `EN_ESPERA`.
- **Estadísticas REST (`/api/estadisticas`)**: `/jugador/{username}` -> Solicita a MongoDB los datos y los suma. *(NOTA: Controlador pendiente de creación).*
- **WebSockets (Puerto 8081 / 9092)**:
    - Pre-Partida: `registrar-usuario`, `solicitar-unirse`, `aceptar-solicitud`, `seleccionar-personaje`.
    - Batalla: `join-room`, `colocar-barcos`, `atacar`, `usar-habilidad`. (Se responde siempre con `gameState`).