# Auditoría de Seguridad — Aislamiento del Backend API

> [!IMPORTANT]
> El objetivo arquitectónico es: **Frontend → Middleware (8080/8082) → Backend API (8081)**
> El Backend API debe ser **inaccesible** para cualquier cliente externo.

---

## 1. Blindaje del Backend API

El Backend API tiene un `InternalApiKeyFilter` que aplica a **TODAS** las rutas sin excepción (`anyRequest().authenticated()`). Cualquier petición sin la cabecera `X-Internal-Key` correcta recibe un `401` inmediato.

**Veredicto: ✅ El Backend API está completamente blindado.**

---

## 2. Endpoints del Backend API y su cobertura en el Middleware

| Endpoint Backend API | Método | Llamado por el Middleware | Desde |
| :--- | :---: | :---: | :--- |
| `POST /api/user/validar` | POST | ✅ | `BackendClient.validarCredenciales()` |
| `POST /api/user/registrar` | POST | ✅ | `BackendClient.registrarUsuario()` |
| `GET  /api/user/verificar-email` | GET | ✅ | `BackendClient.verificarEmail()` |
| `POST /api/user/correo-recuperacion` | POST | ✅ | `BackendClient.enviarCorreoRecuperacion()` |
| `PUT  /api/user/password` | PUT | ✅ | `BackendClient.actualizarPassword()` |
| `PUT  /api/user/{username}/password` | PUT | ✅ | `BackendClient.actualizarPasswordByUsername()` |
| `PUT  /api/user/{username}/nickname` | PUT | ✅ | `BackendClient.actualizarUsername()` |
| `PUT  /api/user/{username}/profile-picture` | PUT | ✅ | `BackendClient.actualizarProfilePicture()` |
| `GET  /api/user/{username}/profile` | GET | ✅ | `BackendClient.getProfileByUsername()` |
| `GET  /api/personajes` | GET | ✅ | `BackendClient.getPersonajes()` |
| `POST /api/partidas/crear` | POST | ✅ | `BackendClient.crearPartida()` |
| `PUT  /api/partidas/{id}/estado` | PUT | ✅ | `BackendClient.actualizarEstadoPartida()` |
| `DELETE /api/partidas/{id}` | DELETE | ✅ | `BackendClient.eliminarPartida()` |
| `GET  /api/estadisticas/jugador/{u}` | GET | ✅ | `BackendClient.getEstadisticasJugador()` |
| `POST /api/estadisticas/guardar` | POST | ✅ | `BackendClient.guardarStats()` |
| `GET  /api/partidas` | GET | ⚠️ **NO** | Nadie lo llama — solo admin |
| `GET  /api/partidas/{id}` | GET | ⚠️ **NO** | Nadie lo llama — solo admin |
| `GET  /api/partidas/estado/{e}` | GET | ⚠️ **NO** | Nadie lo llama — solo admin |

> [!NOTE]
> Los tres endpoints de consulta de partidas marcados como ⚠️ son **rutas administrativas** (solo para debuggear / panel de admin). No necesitan cobertura en el Middleware desde el frontend, pero siguen protegidos por `X-Internal-Key`.

---

## 3. Endpoints del Middleware expuestos al Frontend

| Endpoint Middleware | Método | Destino | Auth requerida |
| :--- | :---: | :--- | :---: |
| `POST /api/auth/login` | POST | Genera JWT | ❌ Público |
| `POST /api/auth/register` | POST | → Backend `/user/registrar` | ❌ Público |
| `POST /api/auth/validate-user` | POST | → Backend `/user/validar` | ✅ JWT |
| `POST /api/auth/forgot-password` | POST | → Backend correo | ❌ Público |
| `POST /api/auth/reset-password` | POST | → Backend password | ❌ Público |
| `POST /api/auth/update-password` | POST | → Backend password | ✅ JWT |
| `POST /api/auth/update-nickname` | POST | → Backend nickname | ✅ JWT |
| `POST /api/auth/update-profile-picture` | POST | → Backend profile-pic | ✅ JWT |
| `GET  /api/personajes` | GET | → Backend personajes | ❌ Público |
| `GET  /api/estadisticas/jugador/{u}` | GET | → Backend estadísticas | ✅ JWT |
| `GET  /api/lobby` | GET | LobbyManager (memoria) | ❌ Público |
| `POST /api/lobby` | POST | LobbyManager + Backend crear | ✅ JWT |
| `DELETE /api/lobby/{id}` | DELETE | LobbyManager | ✅ JWT |
| `GET  /api/partidas/sala-activa/{code}` | GET | GameRoomManager | ❌ Público |
| **Socket.IO port 8082** | WS | GameEngine / LobbyManager | ✅ JWT en handshake |

---

## 4. Servicios del Frontend — URLs utilizadas

| Servicio Angular | URL configurada | Puerto | ¿Correcto? |
| :--- | :--- | :---: | :---: |
| `auth.service.ts` → `/api/auth/*` | `localhost:8080` | Middleware | ✅ |
| `auth.service.ts` → `/api/estadisticas/*` | `localhost:8080` | Middleware | ✅ |
| `room.service.ts` → `/api/lobby` | `localhost:8080` | Middleware | ✅ |
| `room.service.ts` → `/api/partidas` | `localhost:8080` | Middleware | ✅ |
| `personaje.service.ts` → `/api/personajes` | `localhost:8080` | Middleware | ✅ |
| `socket.service.ts` → Socket.IO | `localhost:8082` | Middleware WS | ✅ |

---

## 5. Resultado de la Auditoría

| Capa | Estado |
| :--- | :--- |
| **Backend API blindado** (X-Internal-Key en todo) | ✅ Completamente protegido |
| **Frontend solo habla con Middleware** | ✅ Correcto (puerto 8080 / 8082) |
| **Middleware cubre todos los endpoints funcionales** | ✅ Sí |
| **Rutas públicas del Middleware correctamente declaradas** | ✅ Actualizado hoy |
| **Proxy de `/api/personajes` en el Middleware** | ✅ Creado hoy |

> [!TIP]
> Los 3 endpoints de consulta de partidas del Backend (`GET /api/partidas`, `GET /api/partidas/{id}`, `GET /api/partidas/estado/{e}`) están disponibles solo internamente para uso administrativo. Si necesitas exponerlos al frontend en el futuro, añade el proxy correspondiente en el Middleware.

---

## 6. Diagrama de flujo final

```
┌─────────────────────────────────────────────────────────┐
│                     FRONTEND (4200)                      │
│  auth.service.ts / room.service.ts / personaje.service  │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP  → localhost:8080
                        │ WS    → localhost:8082
                        ▼
┌─────────────────────────────────────────────────────────┐
│               MIDDLEWARE (8080 / 8082)                   │
│  AuthController · LobbyController · PersonajesController │
│  EstadisticasController · PartidasController             │
│  SocketService · GameEngine · LobbyManager               │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP + X-Internal-Key
                        │ → localhost:8081
                        ▼
┌─────────────────────────────────────────────────────────┐
│         BACKEND API (8081) — BLINDADO                    │
│  Requiere X-Internal-Key en CADA petición                │
│  UsuarioController · PersonajeController                 │
│  PartidaController · EstadisticasController              │
│  MySQL (3306) + MongoDB (27017)                          │
└─────────────────────────────────────────────────────────┘
```
