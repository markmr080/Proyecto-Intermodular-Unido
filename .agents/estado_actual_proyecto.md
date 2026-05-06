# 📊 Estado Actual del Proyecto — Warhammer Battleship

> **Generado:** 2026-05-06  
> **Fuentes de referencia:** `agents.md`, `plan_implementacion.md`, `arquitectura_seguridad_middleware.md`  
> **Método:** Auditoría cruzada del código fuente real vs documentación planificada

---

## 1. Resumen Ejecutivo

| Área | Estado |
|------|--------|
| **Seguridad JWT + Middleware** | ✅ Implementado y funcional |
| **Fingerprinting (JWT)** | ✅ Implementado (no estaba en docs originales — es un plus) |
| **Gestión de Usuarios (CRUD)** | ✅ Implementado |
| **Sistema de Email (SMTP)** | ✅ Implementado |
| **Recuperación de Contraseña** | ✅ Implementado |
| **Juego en tiempo real (Sockets)** | ⚠️ Parcialmente implementado |
| **Migración de Entidades (nuevo esquema)** | ❌ No implementado |
| **MongoDB para Estadísticas** | ❌ No implementado |
| **Entidades nuevas (Personaje, Flota, Catálogo)** | ❌ No implementado |
| **Frontend adaptado al nuevo esquema** | ❌ No implementado |

> [!IMPORTANT]
> **Ninguna de las 7 fases del `plan_implementacion.md` ha sido ejecutada.** El código sigue en el estado previo al plan. Todas las entidades, DTOs, servicios y repositorios usan la nomenclatura antigua (`nickname`, `password`, `BarcoEntity`, stats en MySQL, etc.).

---

## 2. Auditoría de Seguridad vs `arquitectura_seguridad_middleware.md`

### ✅ Implementado correctamente

| Concepto documentado | Estado en código | Archivo |
|-----|------|-------|
| Backend blindado: solo `/api/auth/login` público | ✅ | [SecurityConfig.java](file:///c:/Users/User%2001/Desktop/Cosas%20de%20clase/Proyecto%20Intermodular%20Unido/Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/middleware/config/security/SecurityConfig.java) |
| `middleware_admin` creado en @PostConstruct | ✅ | [UsuarioService.java](file:///c:/Users/User%2001/Desktop/Cosas%20de%20clase/Proyecto%20Intermodular%20Unido/Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/api/service/impl/UsuarioService.java#L27-L41) |
| Solo `middleware_admin` puede hacer login | ✅ | [AuthService.java](file:///c:/Users/User%2001/Desktop/Cosas%20de%20clase/Proyecto%20Intermodular%20Unido/Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/middleware/service/impl/AuthService.java#L26-L28) |
| Token JWT stateless con 1 hora de vida | ✅ | `application.properties` → `jwt.expiration.ms=3600000` |
| Sliding Session (renovación a los 10 min) | ✅ | [JwtFilter.java](file:///c:/Users/User%2001/Desktop/Cosas%20de%20clase/Proyecto%20Intermodular%20Unido/Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/middleware/config/security/JwtFilter.java#L70-L82) — cabecera `Token-Nuevo` |
| Cabecera `Token-Nuevo` con `Access-Control-Expose-Headers` | ✅ | JwtFilter.java L78-79 |
| Registro intermediado por middleware (no usuario directo) | ✅ | Frontend usa `withMiddlewareToken()` en [auth.service.ts](file:///c:/Users/User%2001/Desktop/Cosas%20de%20clase/Proyecto%20Intermodular%20Unido/front-conectado-main/src/app/services/auth.service.ts#L160-L167) |
| Identificación del jugador en JSON (no en token) | ✅ | Todos los DTOs incluyen `nickname` en el body |

### 🔵 Mejora no documentada (Extra)

| Característica | Detalle |
|----|----|
| **Fingerprinting SHA-256** | El frontend genera un hash SHA-256 del navegador y lo envía como `X-Fingerprint`. El token JWT almacena el fingerprint en el claim `fp`. El `JwtFilter` valida que coincidan. **Esto NO está en `arquitectura_seguridad_middleware.md`**, pero es una capa de seguridad adicional implementada correctamente. |

### ⚠️ Advertencia

| Punto del documento | Problema detectado |
|---|---|
| §6 — *"El Middleware está obligado a identificar al jugador en el JSON"* | En el `GameSocketController`, el jugador se identifica por `jugadorId` (String), pero **no hay validación** de que ese ID corresponda a un usuario real en MySQL. Cualquier string se acepta. |

---

## 3. Estado de las Entidades vs `plan_implementacion.md`

### 3.1 `UsuarioEntity` — FASE 2 ❌ Sin cambios

| Campo planificado | Estado actual | Acción pendiente |
|---|---|---|
| `id` → `@Column(name="id_usuario")` | ❌ Sin `@Column` — usa nombre auto | Añadir `@Column` |
| `nickname` → `username` | ❌ Sigue siendo `nickname` | Renombrar campo + columna |
| `password` → `passwordHash` | ❌ Sigue siendo `password` | Renombrar campo + columna |
| `email` | ✅ Sin cambio necesario | — |
| `role` (conservar) | ✅ Presente | — |
| `profilePicture` (conservar) | ✅ Presente | — |

> [!WARNING]
> **Cascada total:** El renombramiento de `nickname`→`username` afecta a **15+ archivos** entre backend y frontend. Ver sección 5 del plan.

### 3.2 `PartidaEntity` — FASE 3 ❌ Sin cambios

| Estado actual | Estado planificado | Diferencia |
|---|---|---|
| `jugador1` (String) | `id_host` (FK → UsuarioEntity) | ❌ Sin relación FK |
| `jugador2` (String) | Eliminado (va en PARTIDA_STATS) | ❌ Aún existe |
| `nombreJugador1/2`, `avatarJugador1/2` | Eliminados | ❌ Aún existen (6 campos sobrantes) |
| `codigoSala` | Eliminado (in-memory via sockets) | ❌ Aún existe |
| `turno` | Eliminado (in-memory via GameState) | ❌ Aún existe |
| No existe `ganador_id` | FK → UsuarioEntity | ❌ No creado |
| No existe `fecha_inicio` | LocalDateTime | ❌ No creado |
| No existe `fecha_fin` | LocalDateTime | ❌ No creado |
| `estado` sin `@Enumerated` | `@Enumerated(EnumType.STRING)` | ❌ Falta anotación |

### 3.3 `EstadoPartida` — FASE 3 ❌ Sin cambios

```diff
 // ACTUAL:
 ESPERANDO, EN_CURSO, FINALIZADA

 // PLANIFICADO:
-ESPERANDO
+EN_ESPERA
 EN_CURSO
 FINALIZADA
+CAIDA_SERVIDOR
```

### 3.4 `BarcoEntity` — FASE 6 ❌ Sin cambios

| Estado actual | Estado planificado | Diferencia |
|---|---|---|
| Tabla `barcos` | Tabla `barcos_catalogo` | ❌ No renombrada |
| `tipoBarco` | `nombre` | ❌ No renombrado |
| `cuantoAtaque`, `cuantaDefensa` | Eliminados | ❌ Aún existen |
| No existe `tamano` | `tamano` (int) | ❌ No creado |

### 3.5 `PartidasStatsEntity` — FASE 5 ❌ Sin cambios

| Estado actual | Estado planificado | Diferencia |
|---|---|---|
| JPA Entity (MySQL) | MongoDB Document | ❌ Sigue en MySQL |
| `nickname` (String) | `idUsuario` (Long, FK) | ❌ Sin FK |
| `partidasGanadas` | Calculado desde partidas | ❌ Sigue como campo |
| `impactosAcertados/Fallados` | `hitsAcertados/Fallados` | ❌ No renombrado |
| No existe `idPartida` | Referencia a partida | ❌ No creado |
| No existe `idPersonaje` | Referencia a personaje | ❌ No creado |
| No existe `barcosHundidos` | Nuevo campo | ❌ No creado |

### 3.6 Entidades NUEVAS — FASE 4 ❌ No creadas

| Entidad | Estado |
|---|---|
| `PersonajeEntity` | ❌ No existe |
| `BarcosCatalogoEntity` | ❌ No existe |
| `PersonajeFlotaEntity` | ❌ No existe |
| `PersonajeFlotaId` | ❌ No existe |
| `PartidaStatsDocument` (MongoDB) | ❌ No existe |
| `StatsAgregadasDTO` | ❌ No existe |

---

## 4. Estado de la Configuración — FASE 1

### 4.1 `pom.xml`

| Dependencia | Estado |
|---|---|
| `spring-boot-starter-data-mongodb` | ✅ Presente (línea 39) |
| `spring-boot-starter-data-jpa` | ✅ Presente |
| `spring-boot-starter-security` | ✅ Presente |
| `netty-socketio 1.7.19` | ✅ Presente |
| `jjwt 0.12.6` | ✅ Presente |
| `spring-boot-starter-mail` | ✅ Presente |

### 4.2 `application.properties`

| Configuración | Estado |
|---|---|
| MySQL (localhost:3306) | ✅ Configurado |
| `hibernate.ddl-auto=update` | ✅ Configurado |
| JWT (secret, expiration, renewal) | ✅ Configurado |
| SMTP Gmail | ✅ Configurado |
| **MongoDB URI** | ❌ **No configurado** |
| **MongoDB database** | ❌ **No configurado** |

### 4.3 Configuración JPA/Mongo separada

| Archivo | Estado |
|---|---|
| `MongoConfig.java` | ❌ No existe |
| `JpaConfig.java` | ❌ No existe |

> [!NOTE]
> Aunque la dependencia de MongoDB está en el `pom.xml`, **no hay configuración de conexión** ni separación de contextos JPA/Mongo. Spring Boot probablemente auto-configura Mongo pero sin URI definida, apuntaría a `localhost:27017` por defecto y fallaría silenciosamente si no hay instancia corriendo.

---

## 5. Estado de Repositorios

| Repositorio | Estado actual | Estado planificado |
|---|---|---|
| `UsuarioRepository` | ✅ Existe — usa `findByNickname` | ⏳ Debe migrar a `findByUsername` |
| `PartidaRepository` | ✅ Existe — tiene `findByCodigoSala` | ⏳ Eliminar `findByCodigoSala`, añadir `findByEstado`, `findByHost` |
| `PartidasStatsRepository` | ✅ Existe (MySQL/JPA) | ⏳ **Eliminar** — reemplazar por MongoRepository |
| `BarcoRepository` | ✅ Existe | ⏳ **Eliminar** — reemplazar por `BarcosCatalogoRepository` |
| `EstadisticasRepository` (Mongo) | ⚠️ Existe pero **vacía** (clase sin contenido) | ⏳ Implementar como `MongoRepository<PartidaStatsDocument, String>` |
| `PersonajeRepository` | ❌ No existe | ⏳ Crear |
| `BarcosCatalogoRepository` | ❌ No existe | ⏳ Crear |
| `PersonajeFlotaRepository` | ❌ No existe | ⏳ Crear |

---

## 6. Estado de Servicios

| Servicio | Estado actual | Alineado con plan |
|---|---|---|
| `EstadisticasService` | Usa `PartidasStatsRepository` (MySQL) con `findByNickname` | ❌ Debe migrar a MongoDB |
| `IEstadisticasService` | Métodos `getStats(nickname)`, `actualizarStats(...)` | ❌ Debe cambiar a `getStatsAgregadas`, `guardarStatsPartida`, `getHistorial` |
| `UsuarioService` | Usa `getNickname()`, `setNickname()`, `getPassword()` | ❌ Debe migrar a `username/passwordHash` |
| `AuthService` (middleware) | Usa `loginDTO.getNickname()`, `admin.getNickname()` | ❌ Debe migrar |
| `CharacterFactory` | **Hardcoded** (Artillero, Comandante) | ❌ Debe leer de tablas PERSONAJES + PERSONAJE_FLOTA |

---

## 7. Estado de DTOs

| DTO | Campo actual | Campo planificado | Estado |
|---|---|---|---|
| `LoginDTO` | `nickname` | `username` | ❌ |
| `RegistroDTO` | `nickname` | `username` | ❌ |
| `UpdateNicknameDTO` | usa `nickname` internamente | — | ⚠️ Revisar |
| `UpdatePasswordDTO` | usa `nickname` | `username` | ❌ |
| `UpdateProfilePictureDTO` | usa `nickname` | `username` | ❌ |
| `ActualizarStatsDTO` | Estructura antigua | Reestructurar completamente | ❌ |
| `StatsAgregadasDTO` (record) | No existe | Crear | ❌ |

---

## 8. Estado del Frontend

| Componente/Archivo | Estado |
|---|---|
| `auth.service.ts` — `StatsDTO` | ❌ Usa `nickname`, `impactosAcertados`, `impactosFallados` — debe migrar a `username`, `hitsAcertados`, `hitsFallados`, `barcosHundidos`, `partidasJugadas` |
| `auth.service.ts` — `UserDB` | ✅ Ya usa `username` (coherente con plan) |
| `auth.service.ts` — bodies de login/register | ❌ Envía `{ nickname: ... }` al backend — debe ser `{ username: ... }` |
| `auth.service.ts` — `getUserStats()` | ❌ Consulta a MySQL (`/api/estadisticas/jugador/{nickname}`) — debe apuntar al nuevo endpoint Mongo |
| `perfil` component | ❌ Usa `stats.impactosAcertados` — debe adaptarse |
| `socket.service.ts` | ⚠️ Existe pero se desconoce si está actualizado al esquema de sockets actual |
| Token en `sessionStorage` | ⚠️ `agents.md` dice `localStorage`, código usa `sessionStorage` — **discrepancia de documentación** |

> [!WARNING]
> **Discrepancia doc vs código:** `agents.md` §3 dice *"El token JWT se guarda en el localStorage"*, pero `auth.service.ts` usa `sessionStorage`. El código actual (sessionStorage) es **más seguro** porque el token se borra al cerrar pestaña. El documento `agents.md` debería actualizarse.

---

## 9. Juego en Tiempo Real (Sockets)

| Funcionalidad | Estado |
|---|---|
| Servidor Socket.IO (Netty) en puerto 9092 | ✅ Configurado en `SocketIOConfig.java` |
| `GameSocketController` — join-room, atacar, usar-habilidad, colocar-barcos | ✅ Implementado |
| `GameEngine`, `GameState`, `Player`, `GameCharacter` | ✅ Implementados |
| `CharacterFactory` (Artillero, Comandante) | ✅ Implementado (hardcoded) |
| **Persistencia de partida al finalizar** | ❌ No implementada — `GameSocketController` no llama a ningún repository al terminar una partida |
| **Guardado de stats al finalizar partida** | ❌ No implementado |
| `GameRoomManager` — gestión in-memory | ✅ Funcional |
| `TurnTimerService` | ✅ Implementado |

> [!CAUTION]
> **Riesgo crítico:** Al finalizar una partida, no se guarda ningún dato en base de datos. Ni el resultado ni las estadísticas se persisten. El plan exige que al finalizar:
> 1. Se cree/actualice un registro en `PARTIDAS` (MySQL) con `ganador_id` y `fecha_fin`
> 2. Se guarde un documento `PARTIDA_STATS` por jugador en MongoDB

---

## 10. Checklist Global del `plan_implementacion.md`

### Archivos a CREAR

| Archivo | Estado |
|---|---|
| `api/config/MongoConfig.java` | ❌ |
| `api/config/JpaConfig.java` | ❌ |
| `api/model/entity/PersonajeEntity.java` | ❌ |
| `api/model/entity/BarcosCatalogoEntity.java` | ❌ |
| `api/model/entity/PersonajeFlotaEntity.java` | ❌ |
| `api/model/entity/PersonajeFlotaId.java` | ❌ |
| `api/model/entity/PartidaStatsDocument.java` (Mongo) | ❌ |
| `api/model/dto/StatsAgregadasDTO.java` | ❌ |
| `api/repository/mysql/PersonajeRepository.java` | ❌ |
| `api/repository/mysql/BarcosCatalogoRepository.java` | ❌ |
| `api/repository/mysql/PersonajeFlotaRepository.java` | ❌ |

### Archivos a MODIFICAR

| Archivo | Estado |
|---|---|
| `application.properties` — Config MongoDB | ❌ |
| `UsuarioEntity.java` — Renombrar campos | ❌ |
| `PartidaEntity.java` — Reestructuración completa | ❌ |
| `EstadoPartida.java` — Añadir valores enum | ❌ |
| `UsuarioRepository.java` — Adaptar métodos | ❌ |
| `PartidaRepository.java` — Adaptar métodos | ❌ |
| `EstadisticasRepository.java` — Convertir a MongoRepository | ❌ |
| `UsuarioService.java` — Renombramiento de campos | ❌ |
| `IUsuarioService.java` — Adaptar interfaz | ❌ |
| `EstadisticasService.java` — Migrar a MongoDB | ❌ |
| `IEstadisticasService.java` — Actualizar interfaz | ❌ |
| `AuthService.java` — Adaptar a nuevos nombres | ❌ |
| `AuthController.java` — Adaptar a nuevos nombres | ❌ |
| `PartidaController.java` — Adaptar al nuevo esquema | ❌ |
| `GameSocketController.java` — Integrar persistencia | ❌ |
| `LoginDTO.java` — nickname → username | ❌ |
| `RegistroDTO.java` — nickname → username | ❌ |
| `ActualizarStatsDTO.java` — Reestructurar | ❌ |
| `UpdateNicknameDTO.java` — Adaptar | ❌ |
| `UpdatePasswordDTO.java` — Adaptar | ❌ |
| `UpdateProfilePictureDTO.java` — Adaptar | ❌ |
| Frontend `auth.service.ts` — StatsDTO + nombres | ❌ |
| Frontend `perfil.ts` — Adaptar stats | ❌ |
| Frontend `perfil.html` — Nuevos campos stats | ❌ |

### Archivos a ELIMINAR

| Archivo | Estado |
|---|---|
| `BarcoEntity.java` | ❌ Aún existe |
| `BarcoRepository.java` | ❌ Aún existe |
| `PartidasStatsEntity.java` | ❌ Aún existe |
| `PartidasStatsRepository.java` (MySQL) | ❌ Aún existe |

---

## 11. Prioridades de Implementación Recomendadas

Siguiendo el orden del plan (`FASE 1 → 4 → 2 → 3 → 5 → 6 → 7`):

| Prioridad | Fase | Descripción | Impacto |
|---|---|---|---|
| 🔴 1º | **Fase 1** | Configurar MongoDB en `application.properties` + crear `MongoConfig` + `JpaConfig` | Base para todo lo demás |
| 🔴 2º | **Fase 4** | Crear entidades nuevas (Personaje, BarcosCatalogo, PersonajeFlota) | Sin riesgo de rotura |
| 🔴 3º | **Fase 2** | Migrar `UsuarioEntity` (nickname→username) | **Alto riesgo:** cascada a 15+ archivos |
| 🔴 4º | **Fase 3** | Reestructurar `PartidaEntity` | Depende de Fase 2 |
| 🟡 5º | **Fase 5** | Migrar estadísticas a MongoDB | Depende de entidades nuevas |
| 🟢 6º | **Fase 6** | Eliminar `BarcoEntity` vieja | Limpieza |
| 🟢 7º | **Fase 7** | Actualizar frontend Angular | Último paso |

---

## 12. Discrepancias entre Documentos

| Discrepancia | Detalle |
|---|---|
| **Token storage** | `agents.md` dice `localStorage`, código real usa `sessionStorage` (más seguro). Actualizar doc. |
| **Campos de RegistroDTO** | `agents.md` §4.A menciona `nombre` y `apellidos`, pero ni el código ni el plan tienen esos campos. El registro solo pide `nickname`, `email`, `password`. |
| **MongoDB Atlas vs Local** | `agents.md` dice "MongoDB Atlas (Puerto 27017)", pero el plan de implementación asume MongoDB local. Decidir cuál usar. |
| **Endpoints de agents.md** | `agents.md` lista `GET /api/usuarios` y `GET /api/estadisticas/ranking` como principales, pero ninguno de los dos existe actualmente como endpoint en los controllers. |
| **Fingerprinting** | Implementado en el código pero no documentado en ninguno de los tres archivos de referencia. Debería documentarse. |
