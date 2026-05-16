# Documentación Técnica — Warhammer Battleship CLMM

---

## 3. Arquitectura del Sistema

### 3.1 Descripción General de la Infraestructura Distribuida

El proyecto implementa una **arquitectura de microservicios en tres capas** con separación estricta de responsabilidades. Ninguna capa puede saltarse a la anterior: el frontend nunca accede directamente a la base de datos ni al Backend API, y el Backend API nunca es accesible desde el exterior.

```
┌──────────────────────────────────────────────────────────┐
│         CLIENTE — Frontend Angular (Puerto 4200)          │
│    HTTP → :8080   |   WebSocket (Socket.IO) → :8082       │
└─────────────────────────┬────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────┐
│     SERVIDOR PSP — Middleware Spring Boot (Puerto 8080)   │
│  Seguridad JWT · Lógica de negocio · Socket.IO :8082      │
└─────────────────────────┬────────────────────────────────┘
                          │  HTTP + X-Internal-Key
┌─────────────────────────▼────────────────────────────────┐
│       API REST — Backend Spring Boot (Puerto 8081)        │
│   MySQL :3306 (datos relacionales) · MongoDB :27017       │
└──────────────────────────────────────────────────────────┘
```

### 3.2 Servidor Central — Middleware (PSP)

El **Middleware** (`Middleware_clmm`) es el núcleo del sistema. Actúa simultáneamente como servidor de seguridad HTTP y como servidor de eventos en tiempo real mediante Socket.IO.

**Tecnología:** Spring Boot 3 + Netty-SocketIO  
**Puertos:** `8080` (REST) y `8082` (WebSocket/Socket.IO)  
**Responsabilidades:**

| Módulo | Clase principal | Función |
|--------|----------------|---------|
| Seguridad JWT | `JwtFilter.java` | Valida el token en cada petición HTTP |
| Autenticación | `AuthController.java` | Login, registro, recuperación de contraseña |
| Lógica de sala | `LobbyManager.java` | Estado en memoria de las salas de espera |
| Motor de juego | `GameEngine.java` | Aplica reglas: disparos, habilidades, victoria |
| Gestión partidas | `GameRoomManager.java` | Ciclo de vida de las partidas activas |
| Comunicación API | `BackendClient.java` | Cliente HTTP hacia el Backend API |
| Eventos Socket | `SocketService.java` | Manejo de todos los eventos WebSocket |

#### Ciclo de vida de una partida (gestionado por el Middleware):

1. El host crea una sala → `POST /api/lobby` → se persiste en MySQL mediante `BackendClient.crearPartida()`.
2. El rival solicita unirse → evento Socket `solicitar-unirse` → `nueva-solicitud` al host.
3. El host acepta → `aceptar-solicitud` → ambos se redirigen a la pantalla de selección de personaje.
4. Ambos jugadores envían `join-room` → `GameEngine` se inicializa con los dos `Player`.
5. Ambos envían `colocar-barcos` → cuando los dos están listos, la fase pasa a `COMBATE`.
6. Turnos de ataque mediante evento `atacar` → `procesarDisparo()` → se emite `gameState` a ambos.
7. Al detectar victoria, `finalizarJuego()` → guarda estadísticas en MongoDB y actualiza estado en MySQL.

#### Gestión de desconexiones:

Cuando un jugador se desconecta durante una partida, el servidor lanza un **hilo con 60 segundos de gracia**. Si el jugador vuelve a unirse dentro del plazo (evento `join-room`), la partida continúa exactamente desde el mismo estado. Si no regresa, el rival es declarado ganador automáticamente.

### 3.3 Cliente Multiplataforma — Angular (PWA)

**Tecnología:** Angular 17 + Socket.IO Client  
**Plataformas soportadas:** Navegador web de escritorio y móvil (diseño responsive). La arquitectura del frontend está preparada para su transformación en una PWA (Progressive Web App) mediante `@angular/service-worker`.

**Estructura de servicios del frontend:**

| Servicio Angular | URL destino | Propósito |
|-----------------|-------------|-----------|
| `AuthService` | `localhost:8080/api/auth/*` | Login, registro, gestión de cuenta |
| `AuthService` | `localhost:8080/api/estadisticas/*` | Consulta de estadísticas del jugador |
| `RoomService` | `localhost:8080/api/lobby` | Listar y crear salas |
| `PersonajeService` | `localhost:8080/api/personajes` | Catálogo de personajes |
| `SocketService` | `localhost:8082` (Socket.IO) | Toda la lógica de partida en tiempo real |

El interceptor HTTP `auth.interceptor.ts` añade automáticamente en **todas las peticiones** salientes:
- `Authorization: Bearer <token>` — si el usuario tiene sesión activa.
- `X-Fingerprint: <hash>` — huella SHA-256 del navegador para vincular el token al dispositivo.

### 3.4 Protocolo de Comunicación — Mensajes Socket.IO (JSON)

Todos los mensajes entre cliente y servidor se intercambian en formato JSON. El servidor emite `gameState` para sincronizar el estado completo de la partida tras cada acción.

#### Eventos del Cliente → Servidor

| Evento | Payload JSON | Descripción |
|--------|-------------|-------------|
| `registrar-usuario` | `"username"` | Asocia el socket con el usuario |
| `join-lobby` | `"codigoSala"` | Se une a la sala del lobby |
| `solicitar-unirse` | `{ codigoSala, requesterId, requesterName }` | Solicita unirse a una sala |
| `aceptar-solicitud` | `{ codigoSala, requesterId, requesterName }` | Host acepta al solicitante |
| `rechazar-solicitud` | `{ requesterId, mensaje }` | Host rechaza la solicitud |
| `iniciar-partida` | `{ codigoSala }` | Inicia el flujo de selección de personaje |
| `seleccionar-personaje` | `{ codigoSala, jugadorId, personajeId }` | Notifica la elección de personaje |
| `join-room` | `{ roomCode, jugadorId, jugadorNombre, personajeId }` | Entra a la partida activa |
| `colocar-barcos` | `{ roomCode, jugadorId, tablero: CellStatus[][] }` | Envía el tablero inicial |
| `atacar` | `{ roomCode, jugadorId, x, y }` | Disparo a coordenada (x, y) |
| `usar-habilidad` | `{ roomCode, jugadorId, habilidadId, x, y }` | Activa habilidad especial |
| `rendirse` | `{ roomCode, jugadorId }` | Abandona la partida |
| `abandonar-sala` | `{ codigoSala, userId }` | Sale del lobby |

#### Eventos del Servidor → Cliente

| Evento | Payload | Descripción |
|--------|---------|-------------|
| `gameState` | Objeto `GameState` completo | Estado actualizado de la partida |
| `nueva-solicitud` | `{ requesterId, requesterName }` | Llega al host cuando alguien quiere unirse |
| `solicitud-aceptada` | `"codigoSala"` | Llega al solicitante cuando es aceptado |
| `solicitud-rechazada` | `"mensaje"` | Llega al solicitante cuando es rechazado |
| `jugador-unido` | datos del jugador | Confirma unión a la sala |
| `partida-iniciada` | `"codigoSala"` | Señal para ir a selección de personaje |
| `personaje-seleccionado` | datos de selección | Notifica al rival la elección |
| `juego-comenzado` | `"codigoSala"` | Señal para iniciar la fase de colocación |
| `jugador-desconectado` | `"userId\|userId"` | Avisa al rival de la desconexión |
| `jugador-reconectado` | `"userId"` | Avisa al rival de la reconexión |
| `sala-cerrada` | `"mensaje"` | Sala eliminada por desconexión o abandono |
| `partida-cancelada` | `"userId"` | El segundo jugador abandonó antes de iniciar |

#### Estructura del objeto `GameState` (emitido en cada `gameState`):

```json
{
  "juegoActivo": true,
  "fase": "COMBATE",
  "turnoActualId": "Markmr081",
  "ganadorId": null,
  "mensajeEstado": "¡Impacto en (3,5)!",
  "jugador1": {
    "id": "Markmr081",
    "nombre": "Markmr081",
    "vidas": 12,
    "hitsAcertados": 4,
    "hitsFallados": 7,
    "barcosHundidos": 1,
    "tablero": [["AGUA","BARCO",...], ...],
    "personaje": {
      "nombre": "Aranessa",
      "habilidades": [...]
    }
  },
  "jugador2": { ... }
}
```

---

## 4. Diseño de Base de Datos y Persistencia

### 4.1 Estrategia de Persistencia Dual

El sistema utiliza **dos motores de base de datos** con propósitos diferenciados:

- **MySQL 8.0** — Datos relacionales y transaccionales: usuarios, partidas, catálogo de personajes.
- **MongoDB 7.0** — Estadísticas de partidas (documentos flexibles, optimizados para consultas de ranking).

### 4.2 Modelo Entidad-Relación (MySQL)

```
┌─────────────────────┐       ┌───────────────────────┐
│       USUARIOS       │       │       PARTIDAS         │
│─────────────────────│       │───────────────────────│
│ PK id_usuario (BIGINT)│◄────┤ FK id_host → id_usuario│
│    username (VARCHAR) │      │ FK ganador_id → id_usr │
│    email    (VARCHAR) │      │ PK id_partida (BIGINT) │
│    password_hash      │      │    estado (ENUM)       │
│    role     (VARCHAR) │      │    fecha_inicio         │
│    profile_picture    │      │    fecha_fin            │
└─────────────────────┘       └───────────────────────┘

┌─────────────────────┐       ┌───────────────────────┐
│     PERSONAJES      │       │    BARCOS_CATALOGO     │
│─────────────────────│       │───────────────────────│
│ PK id_personaje     │       │ PK id_barco_tipo       │
│    nombre (VARCHAR) │       │    nombre (VARCHAR)    │
└────────┬────────────┘       │    tamano (INT)        │
         │                    └──────────┬────────────┘
         │      ┌────────────────────────┘
         │      │
         ▼      ▼
┌─────────────────────────────────────────────────────┐
│                   PERSONAJE_FLOTA                    │
│─────────────────────────────────────────────────────│
│ PK/FK id_personaje → PERSONAJES.id_personaje        │
│ PK/FK id_barco_tipo → BARCOS_CATALOGO.id_barco_tipo │
│       cantidad (INT)                                │
└─────────────────────────────────────────────────────┘
```

**Descripción de tablas:**

| Tabla | Filas clave | Propósito |
|-------|-------------|-----------|
| `USUARIOS` | `id_usuario`, `username`, `email`, `password_hash`, `role`, `profile_picture` | Registro y autenticación. El campo `role` distingue entre `ROLE_USER` y `middleware_admin`. |
| `PARTIDAS` | `id_partida`, `id_host` (FK), `ganador_id` (FK), `estado`, `fecha_inicio`, `fecha_fin` | Historial de partidas. El estado es un ENUM: `EN_ESPERA`, `EN_CURSO`, `FINALIZADA`, `CAIDA_SERVIDOR`. |
| `PERSONAJES` | `id_personaje`, `nombre` | Catálogo de personajes jugables: Wulfrik, Aislinn, Lokhir, Aranessa, Ikit Claw, Balthasar Gelt. |
| `BARCOS_CATALOGO` | `id_barco_tipo`, `nombre`, `tamano` | Tipos de barco disponibles y su tamaño en casillas. |
| `PERSONAJE_FLOTA` | `id_personaje` + `id_barco_tipo` (PK compuesta), `cantidad` | Relación N:M entre personajes y sus flotas, con la cantidad de cada tipo de barco. |

### 4.3 Colección MongoDB — Estadísticas

La colección `partida_stats` almacena **un documento por jugador por partida** (patrón insert-only). Se desnormaliza el `username` para evitar joins entre MongoDB y MySQL en consultas de ranking.

**Estructura del documento `PartidaStatsDocument`:**

```json
{
  "_id": "6a063b47bf03b490b0aac09c",
  "idPartida": 12,
  "idUsuario": 3,
  "idPersonaje": 5,
  "hitsAcertados": 17,
  "hitsFallados": 8,
  "barcosHundidos": 5,
  "ganador": true,
  "username": "Markmr081",
  "_class": "com.cifpaviles...PartidaStatsDocument"
}
```

**Método de cálculo de estadísticas agregadas (ranking):**

El endpoint `GET /api/estadisticas/jugador/{username}` del Middleware retorna un `StatsAgregadasDTO` calculado al vuelo consultando todos los documentos del usuario en MongoDB:

```
partidasJugadas  = COUNT(documentos del usuario)
partidasGanadas  = COUNT(documentos donde ganador = true)
hitsAcertados    = SUM(hitsAcertados)
hitsFallados     = SUM(hitsFallados)
barcosHundidos   = SUM(barcosHundidos)
punteria         = (hitsAcertados * 100.0) / (hitsAcertados + hitsFallados) %
```

### 4.4 Servidor API REST — Capa de Persistencia

El **Backend API** (`Proyecto-Final-Multiplataforma-BackApi-master`) actúa como servidor de persistencia puro. **No tiene interfaz de usuario ni lógica de negocio** — solo gestiona el acceso a las bases de datos.

**Tecnología:** Spring Boot 3 + Spring Data JPA (MySQL) + Spring Data MongoDB  
**Puerto:** `8081` (interno, nunca expuesto al exterior)

**Protección:** El filtro `InternalApiKeyFilter` intercepta **TODAS** las peticiones y rechaza con `HTTP 401` cualquiera que no incluya la cabecera `X-Internal-Key` con el valor correcto. Esta clave es un secreto compartido exclusivamente entre el Middleware y el Backend.

**Flujo completo de persistencia (ejemplo: fin de partida):**

```
GameEngine.finalizarJuego()
    └─► BackendClient.guardarStats(idPartida, username, ...)
            └─► POST http://backend-api:8081/api/estadisticas/guardar
                    [Header: X-Internal-Key: ClmmDockerSecretKey2026]
                        └─► EstadisticasController → guardarStatsPartida()
                                └─► mongoStatsRepository.save(doc)
                                        └─► MongoDB ✓
    └─► BackendClient.actualizarEstadoPartida(idPartida, "FINALIZADA", ganador)
            └─► PUT http://backend-api:8081/api/partidas/{id}/estado
                        └─► PartidaController → partidaRepository.save()
                                └─► MySQL ✓
```

---

## 5. Despliegue y DevOps

### 5.1 Containerización con Docker

El proyecto incluye un fichero `docker-compose.yml` que orquesta **5 contenedores independientes**, garantizando que el entorno de producción sea idéntico al de desarrollo:

```yaml
Servicios:
  mysql-db     → MySQL 8.0        (Puerto 3306)
  mongo-db     → MongoDB 7.0      (Puerto 27017)
  backend-api  → Spring Boot JAR  (Puertos 8081, 8082)
  middleware   → Spring Boot JAR  (Puertos 8080, 8083)
  frontend     → Nginx + Angular  (Puerto 4200 → 80)
```

**Ventajas de la containerización:**
- **Portabilidad:** El mismo `docker-compose up` funciona en cualquier máquina con Docker instalado.
- **Aislamiento de red:** Los servicios se comunican por nombre de contenedor (`backend-api`, `mysql-db`) en lugar de IPs, evitando problemas de resolución.
- **Variables de entorno:** Toda la configuración sensible (credenciales, URLs, claves API) se inyecta mediante variables de entorno en el `docker-compose.yml`, sin estar hardcodeadas en el código fuente.
- **Orden de arranque:** La directiva `depends_on` garantiza que MySQL y MongoDB estén disponibles antes de que arranque el Backend, y el Backend antes que el Middleware.

**Variables de entorno críticas inyectadas en tiempo de ejecución:**

```
INTERNAL_API_KEY   → Clave compartida entre Backend y Middleware
BACKEND_API_URL    → URL interna del Backend (http://backend-api:8081)
APP_FRONTEND_URL   → URL pública del frontend (para CORS)
SPRING_DATASOURCE_URL → Cadena de conexión a MySQL
SPRING_DATA_MONGODB_URI → URI de conexión a MongoDB
```

### 5.2 Infraestructura Cloud — Microsoft Azure

El proyecto está desplegado en **Microsoft Azure** mediante una **Máquina Virtual Linux** con Docker instalado. La URL pública del proyecto es:

```
https://warhammer-battleship-clmm.polandcentral.cloudapp.azure.com
```

El dominio `polandcentral.cloudapp.azure.com` indica que la VM está ubicada en la región **Poland Central** de Azure.

**Configuración de red en Azure:**

El grupo de seguridad de red (NSG) de Azure expone únicamente los puertos necesarios:
- `80/443` → Frontend Angular (Nginx)
- `8080` → Middleware REST (redirigido por Nginx en producción al proxy inverso)
- `8082` → Socket.IO del Middleware

El puerto `8081` del Backend API **no está expuesto en el NSG**, garantizando que sea inaccesible desde el exterior aunque Docker lo mapee internamente.

### 5.3 Script de Actualización y Despliegue Continuo

El fichero `actualizar.sh` en la raíz del repositorio automatiza el proceso de actualización en el servidor Azure:

```bash
#!/bin/bash
# 1. Descarga los últimos cambios de GitHub
git pull

# 2. Reconstruye las imágenes Docker y reinicia los contenedores
sudo docker compose up -d --build

# 3. Limpia imágenes antiguas para no saturar el disco
sudo docker image prune -f
```

**Flujo de despliegue manual:**
1. El desarrollador hace push a la rama `main` en GitHub.
2. Accede por SSH al servidor Azure.
3. Ejecuta `./actualizar.sh`.
4. Docker descarga los cambios, reconstruye solo los contenedores afectados y los reinicia sin tiempo de inactividad perceptible.

**Para desarrollo local**, el fichero `lanzar_proyecto.bat` (Windows) lanza los tres servicios principales en terminales separadas:

```
BACKEND API  → mvnw.cmd spring-boot:run  (puerto 8081)
MIDDLEWARE   → mvnw.cmd spring-boot:run  (puerto 8080)
FRONTEND     → npm start                 (puerto 4200)
```

### 5.4 CI/CD — GitHub Actions

El repositorio (`markmr080/Proyecto-Intermodular-Unido`) utiliza GitHub como plataforma de control de versiones y colaboración. No se ha implementado un pipeline CI/CD automatizado con GitHub Actions en el momento actual del proyecto; el despliegue se realiza de forma manual mediante el script `actualizar.sh` tras cada push a `main`.

> **Mejora futura identificada:** Implementar un workflow `.github/workflows/deploy.yml` que ejecute automáticamente `test_endpoints.py` en cada pull request y lance el script de despliegue vía SSH al hacer merge a `main`.

---

## 6. Control de Versiones y Calidad

### 6.1 Estrategia de Git

**Repositorio:** `https://github.com/markmr080/Proyecto-Intermodular-Unido`  
**Modelo de ramas:**

| Rama | Propósito |
|------|-----------|
| `main` | Rama principal. Solo recibe código estable y probado mediante Pull Requests. |
| `Desarrollo` | Rama de integración de nuevas funcionalidades del Backend y Frontend. |
| `Desarollo-Middle` | Rama dedicada al desarrollo y migración del Middleware como microservicio independiente. |

**Historial de commits representativo (últimos 25):**

```
da4afcc  Merge pull request #1 from markmr080/Desarollo-Middle
3c315d5  Merge branch 'main' into Desarollo-Middle
82653f2  Guardado temporal antes de resolver conflictos
7fb2fe0  implementar_websocket.md
c221f40  feat: socket implementation refactoring
262a641  docs: add documentation detailing the complete project functionality
9ee73e9  feat: implement backend client and internal API key security filter
6ba3cd8  feat: implement EstadisticasService to aggregate and persist user stats
35a7045  feat: implement middleware service with socket support and game engine
b346870  fix: arreglada habilidad defensiva aislinn
89706a4  feat: add login component styling and backend database configuration
e2bd8b1  feat: add menu component styling and server configuration properties
b0c362f  feat: responsive a partida y mejora para movil
a96dbbc  docs: add technical documentation for Netty-SocketIO integration
e247f3f  feat: add Lokhir character image to public assets
32eeb56  feat: add CSS styles for lobby and game screens
181f63d  feat: add new characters and implement session reconnection logic
c5dac05  feat: implement menu character selection and persistent reconnection
```

**Convención de commits:** Se utiliza **Conventional Commits** de forma parcial:
- `feat:` para nuevas funcionalidades.
- `fix:` para correcciones de bugs.
- `docs:` para documentación.
- Commits de trabajo en progreso con descripciones descriptivas del cambio.

**Resolución de conflictos:** Los conflictos detectados durante el merge de `Desarollo-Middle` a `main` se resolvieron manualmente (commit `82653f2: Guardado temporal antes de resolver conflictos`) y se integraron mediante Pull Request (`da4afcc`) para revisión antes de fusionar.

### 6.2 Pruebas

#### Pruebas de Integración — `test_endpoints.py`

Se ha desarrollado una suite de **11 pruebas de integración de extremo a extremo** en Python que valida el flujo completo `Frontend → Middleware → Backend API → Base de datos`.

**Ejecución:** `python test_endpoints.py` (requiere los tres servicios arrancados)

**Flujo de pruebas:**

```
[STEP 1]  POST /api/auth/login          → Obtiene token JWT del Middleware
[STEP 2]  GET  /api/personajes          → Lista el catálogo de personajes
[STEP 3]  POST /api/auth/register       → Registra un usuario de prueba dinámico
[STEP 4]  POST /api/auth/validate-user  → Valida credenciales del usuario creado
[STEP 5]  GET  /api/user/{user}         → Obtiene el perfil del usuario
[STEP 6]  POST /api/auth/update-nickname → Cambia el username
[STEP 7a] GET  /api/lobby               → Lista salas (antes de crear)
[STEP 7b] POST /api/lobby               → Crea una sala de prueba
[STEP 7c] GET  /api/lobby               → Verifica que la sala aparece en el listado
[STEP 8a] GET  /api/partidas            → Lista todas las partidas en BD
[STEP 8b] GET  /api/partidas/estado/*   → Filtra por cada estado posible
[STEP 8c] GET  /api/partidas/{id}       → Consulta detalle de partida por ID
[STEP 8d] GET  /api/partidas/sala-activa/{code} → Verifica sala activa en memoria
[STEP 9]  GET  /api/estadisticas/jugador/{user} → Consulta estadísticas MongoDB
[STEP 10] GET  /api/game/estado         → Verifica zona segura JWT
[STEP 11] DELETE /api/lobby/{code}      → Limpieza: elimina la sala de prueba
```

Cada prueba imprime el código HTTP de respuesta y el cuerpo JSON, facilitando la detección de regresiones. El usuario de prueba utiliza un timestamp en el nombre (`user_{timestamp}`) para evitar colisiones en ejecuciones consecutivas.

#### Pruebas del Motor de Juego

La lógica de `GameEngine.java` (más de 1100 líneas) ha sido validada funcionalmente mediante **pruebas manuales de partidas reales**, verificando:

- Contabilización correcta de `hitsAcertados` y `hitsFallados` (incluyendo disparos bloqueados por escudos).
- Hundimiento correcto de barcos mediante el algoritmo DFS (`dfsSunkCheck`).
- Activación de habilidades especiales de los 6 personajes con sus efectos correctos.
- Detección y persistencia del ganador al terminar la partida.
- Reconexión funcional sin pérdida de estado.

#### Validación de Seguridad

Se verificó manualmente que:
- Cualquier petición directa al Backend API sin cabecera `X-Internal-Key` devuelve `HTTP 401`.
- Cualquier petición al Middleware a endpoints protegidos sin JWT devuelve `HTTP 401`.
- El fingerprint del navegador (`X-Fingerprint`) vincula el token al dispositivo, invalidándolo si se copia a otra máquina.
