# Plan de Implementación — Actualización de Entidades y MongoDB

## 1. Resumen del Análisis

### 1.1 Esquema Objetivo (Nuevo)

| Tabla | Campos | Almacenamiento |
|-------|--------|----------------|
| `USUARIOS` | id_usuario, username, password_hash, email | MySQL |
| `PARTIDAS` | id_partida, id_host(FK), ganador_id(FK), estado, fecha_inicio, fecha_fin | MySQL |
| `PERSONAJES` | id_personaje, nombre | MySQL |
| `BARCOS_CATALOGO` | id_barco_tipo, nombre, tamano | MySQL |
| `PERSONAJE_FLOTA` | id_personaje(FK), id_barco_tipo(FK), cantidad | MySQL |
| `PARTIDA_STATS` | id_stat, id_partida, id_usuario, id_personaje, hits_acertados, hits_fallados, barcos_hundidos | **MongoDB** |

### 1.2 Estado Actual de Entidades vs Objetivo

#### `UsuarioEntity` — Cambios menores
| Campo Actual | Campo Nuevo | Acción |
|---|---|---|
| `id` (Long) | `id_usuario` (Long) | Renombrar columna |
| `nickname` | `username` | Renombrar campo + columna |
| `email` | `email` | Sin cambio |
| `password` | `password_hash` | Renombrar campo + columna |
| `role` | — | **Conservar** (necesario para lógica middleware_admin) |
| `profilePicture` | — | **Conservar** (usado en frontend, no aparece en esquema nuevo pero es funcionalidad existente) |

> **IMPORTANTE:** Los campos `role` y `profilePicture` no están en el nuevo esquema SQL pero son esenciales para la lógica de autenticación (middleware_admin) y para el perfil del frontend. **Se conservan como campos adicionales.**

#### `PartidaEntity` — Reestructuración completa
| Campo Actual | Campo Nuevo | Acción |
|---|---|---|
| `id` | `id_partida` | Renombrar columna |
| `jugador1` (String) | `id_host` (FK → USUARIOS) | Cambiar a relación `@ManyToOne` |
| `nombreJugador1` | — | **Eliminar** (se obtiene del FK) |
| `avatarJugador1` | — | **Eliminar** |
| `jugador2` (String) | — | **Eliminar** (2° jugador va en PARTIDA_STATS) |
| `nombreJugador2` | — | **Eliminar** |
| `avatarJugador2` | — | **Eliminar** |
| `estado` (enum) | `estado` (ENUM ampliado) | Añadir `CAIDA_SERVIDOR` |
| `codigoSala` | — | **Eliminar** (gestión in-memory via sockets) |
| `turno` | — | **Eliminar** (gestión in-memory via GameState) |
| — | `ganador_id` (FK → USUARIOS) | **Nuevo** campo |
| — | `fecha_inicio` (LocalDateTime) | **Nuevo** campo |
| — | `fecha_fin` (LocalDateTime) | **Nuevo** campo |

#### `BarcoEntity` → `BarcosCatalogoEntity` — Reestructuración
| Campo Actual | Campo Nuevo | Acción |
|---|---|---|
| `id` | `id_barco_tipo` | Renombrar columna |
| `tipoBarco` | `nombre` | Renombrar |
| `cuantoAtaque` | — | **Eliminar** |
| `cuantaDefensa` | — | **Eliminar** |
| — | `tamano` (int) | **Nuevo** campo |

#### `PartidasStatsEntity` → **Migrar a MongoDB**
| Campo Actual (MySQL) | Campo Nuevo (MongoDB) | Acción |
|---|---|---|
| `id` (Long, auto) | `_id` (ObjectId, auto) | Cambiar a MongoDB |
| `nickname` (String) | `idUsuario` (Long) | Cambiar a referencia FK |
| `partidasGanadas` (int) | — | **Eliminar** (se calcula desde partidas) |
| `impactosAcertados` | `hitsAcertados` | Renombrar |
| `impactosFallados` | `hitsFallados` | Renombrar |
| — | `idPartida` (Long) | **Nuevo** — referencia a partida |
| — | `idPersonaje` (Long) | **Nuevo** — referencia a personaje usado |
| — | `barcosHundidos` (int) | **Nuevo** campo |

#### Entidades NUEVAS a crear

| Entidad | Tipo | Descripción |
|---|---|---|
| `PersonajeEntity` | MySQL/JPA | Tabla `PERSONAJES` (id_personaje, nombre) |
| `PersonajeFlotaEntity` | MySQL/JPA | Tabla intermedia `PERSONAJE_FLOTA` con clave compuesta |

---

## 2. Fases de Implementación

### FASE 1 — Configuración MongoDB + Dependencias
**Prioridad: Alta | Riesgo: Medio**

#### 1.1 Verificar dependencia en `pom.xml`
La dependencia `spring-boot-starter-data-mongodb` **ya está presente** (línea 39 del pom.xml). ✅

#### 1.2 Configurar MongoDB en `application.properties`
Añadir la configuración de conexión MongoDB:
```properties
# Configuración MongoDB [ESTADÍSTICAS]
spring.data.mongodb.uri=mongodb://localhost:27017/batalla_naval_stats
spring.data.mongodb.database=batalla_naval_stats
```

> **AVISO:** Se necesita tener MongoDB instalado y corriendo localmente en el puerto 27017.
> Si se usa Docker: `docker run -d -p 27017:27017 --name mongo-stats mongo:7`

#### 1.3 Configurar exclusión de entidades para evitar conflictos JPA/Mongo
Crear clase de configuración para que Spring Boot no intente escanear documentos Mongo como entidades JPA:

**Archivo:** `api/config/MongoConfig.java`
```java
@Configuration
@EnableMongoRepositories(basePackages = "com.cifpaviles.proyectofinal.CLMM.api.repository.mongo")
public class MongoConfig {
    // Configuración dedicada para MongoDB
}
```

**Archivo:** `api/config/JpaConfig.java` (si no existe)
```java
@Configuration
@EnableJpaRepositories(basePackages = {
    "com.cifpaviles.proyectofinal.CLMM.api.repository.mysql",
    "com.cifpaviles.proyectofinal.CLMM.api.model.repository"
})
public class JpaConfig {
    // Asegurar que JPA solo escanea repositorios MySQL
}
```

---

### FASE 2 — Actualizar Entidad `UsuarioEntity`
**Prioridad: Alta | Riesgo: Alto (cascada a muchos archivos)**

#### 2.1 Cambios en `UsuarioEntity.java`
```diff
 @Entity
 @Table(name = "usuarios")
 public class UsuarioEntity {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
+    @Column(name = "id_usuario")
     private Long id;

-    @Column(unique = true, nullable = false)
-    private String nickname;
+    @Column(name = "username", unique = true, nullable = false, length = 100)
+    private String username;

     @Column(unique = true, nullable = false)
     private String email;

-    @Column(nullable = false)
-    private String password;
+    @Column(name = "password_hash", nullable = false, length = 255)
+    private String passwordHash;

     private String role;               // CONSERVAR

     @Column(name = "profile_picture", length = 512)
     private String profilePicture;     // CONSERVAR
```

#### 2.2 Archivos impactados (renombrar `nickname` → `username`, `password` → `passwordHash`)

| Archivo | Cambios necesarios |
|---|---|
| `UsuarioRepository.java` | `findByNickname` → `findByUsername`, `existsByNickname` → `existsByUsername` |
| `UsuarioService.java` | Todos los usos de `getNickname()`, `setNickname()`, `getPassword()`, `setPassword()` |
| `AuthService.java` | `loginDTO.getNickname()` → `loginDTO.getUsername()`, `admin.getNickname()` → `admin.getUsername()` |
| `AuthController.java` | Referencias a `registroDTO.getNickname()`, `user.getNickname()` |
| `LoginDTO.java` | Campo `nickname` → `username` |
| `RegistroDTO.java` | Campo `nickname` → `username` |
| `UpdateNicknameDTO.java` | Campos internos |
| `UpdatePasswordDTO.java` | Campo `nickname` → `username` |
| `UpdateProfilePictureDTO.java` | Campo `nickname` → `username` |
| **Frontend** `auth.service.ts` | Interface `UserDB`, body de login/register, `StatsDTO` |
| **Frontend** `perfil.ts` | `currentUser.username` ya usa username ✅ |

---

### FASE 3 — Reestructurar Entidad `PartidaEntity`
**Prioridad: Alta | Riesgo: Alto**

#### 3.1 Nueva `PartidaEntity.java`
```java
@Entity
@Table(name = "partidas")
public class PartidaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partida")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_host", nullable = false)
    private UsuarioEntity host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ganador_id")
    private UsuarioEntity ganador;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPartida estado;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    // Getters y Setters...
}
```

#### 3.2 Actualizar `EstadoPartida.java`
```diff
 public enum EstadoPartida {
-    ESPERANDO,
-    EN_CURSO,
-    FINALIZADA
+    EN_ESPERA,
+    EN_CURSO,
+    FINALIZADA,
+    CAIDA_SERVIDOR
 }
```

#### 3.3 Archivos impactados

| Archivo | Cambios necesarios |
|---|---|
| `PartidaRepository.java` | Eliminar `findByCodigoSala`, añadir `findByEstado`, `findByHost` |
| `PartidaController.java` | Adaptar a nueva estructura (ya no hay codigoSala) |
| `GameSocketController.java` | La lógica de salas se mantiene in-memory. Al crear/finalizar partida, persistir en MySQL con el nuevo esquema |

---

### FASE 4 — Crear Entidades Nuevas
**Prioridad: Alta | Riesgo: Bajo**

#### 4.1 Crear `PersonajeEntity.java`
```java
@Entity
@Table(name = "personajes")
public class PersonajeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personaje")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // Getters, Setters...
}
```

#### 4.2 Crear `BarcosCatalogoEntity.java` (reemplaza `BarcoEntity`)
```java
@Entity
@Table(name = "barcos_catalogo")
public class BarcosCatalogoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_barco_tipo")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "tamano", nullable = false)
    private int tamano;

    // Getters, Setters...
}
```

#### 4.3 Crear `PersonajeFlotaEntity.java` (tabla intermedia con clave compuesta)
```java
@Entity
@Table(name = "personaje_flota")
@IdClass(PersonajeFlotaId.class)
public class PersonajeFlotaEntity {

    @Id
    @ManyToOne
    @JoinColumn(name = "id_personaje")
    private PersonajeEntity personaje;

    @Id
    @ManyToOne
    @JoinColumn(name = "id_barco_tipo")
    private BarcosCatalogoEntity barcoTipo;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    // Getters, Setters...
}
```

**Crear `PersonajeFlotaId.java`** (clase para la clave compuesta):
```java
public class PersonajeFlotaId implements Serializable {
    private Long personaje;
    private Long barcoTipo;
    // equals() y hashCode()
}
```

#### 4.4 Crear Repositorios
- `PersonajeRepository.java` → `JpaRepository<PersonajeEntity, Long>`
- `BarcosCatalogoRepository.java` → `JpaRepository<BarcosCatalogoEntity, Long>`
- `PersonajeFlotaRepository.java` → `JpaRepository<PersonajeFlotaEntity, PersonajeFlotaId>`

---

### FASE 5 — Migrar Estadísticas a MongoDB
**Prioridad: Alta | Riesgo: Medio**

#### 5.1 Crear documento MongoDB `PartidaStatsDocument.java`
```java
@Document(collection = "partida_stats")
public class PartidaStatsDocument {

    @Id
    private String id;  // ObjectId de MongoDB

    private Long idPartida;      // Referencia a PARTIDAS.id_partida (MySQL)
    private Long idUsuario;      // Referencia a USUARIOS.id_usuario (MySQL)
    private Long idPersonaje;    // Referencia a PERSONAJES.id_personaje (MySQL)

    private int hitsAcertados = 0;
    private int hitsFallados = 0;
    private int barcosHundidos = 0;

    // Campos calculados opcionales (para consultas rápidas)
    private String username;     // Desnormalizado para queries rápidas

    // Constructores, Getters, Setters...
}
```

> **NOTA:** Se incluye el campo `username` desnormalizado para poder hacer consultas de ranking sin necesidad de JOIN con MySQL. Esto es un patrón común en MongoDB.

#### 5.2 Actualizar `EstadisticasRepository.java` (Mongo)
```java
@Repository
public interface EstadisticasRepository extends MongoRepository<PartidaStatsDocument, String> {

    List<PartidaStatsDocument> findByIdUsuario(Long idUsuario);
    List<PartidaStatsDocument> findByIdPartida(Long idPartida);
    List<PartidaStatsDocument> findByUsername(String username);
}
```

#### 5.3 Actualizar `IEstadisticasService.java`
```java
public interface IEstadisticasService {

    /** Obtiene las estadísticas agregadas de un jugador (todas sus partidas). */
    StatsAgregadasDTO getStatsAgregadas(String username);

    /** Guarda las estadísticas de una partida finalizada. */
    PartidaStatsDocument guardarStatsPartida(Long idPartida, Long idUsuario,
        Long idPersonaje, int hitsAcertados, int hitsFallados, int barcosHundidos);

    /** Obtiene el historial de partidas de un jugador. */
    List<PartidaStatsDocument> getHistorial(Long idUsuario);
}
```

#### 5.4 Actualizar `EstadisticasService.java`
La nueva implementación consulta MongoDB para agregar estadísticas:
- `getStatsAgregadas()` → Hace query a Mongo, suma hits, cuenta victorias cruzando con `PartidaRepository`
- `guardarStatsPartida()` → Guarda un nuevo documento en Mongo al finalizar cada partida
- `getHistorial()` → Devuelve lista de stats por usuario

#### 5.5 Crear DTO `StatsAgregadasDTO.java`
```java
public record StatsAgregadasDTO(
    String username,
    int partidasJugadas,
    int partidasGanadas,
    int hitsAcertados,
    int hitsFallados,
    int barcosHundidos,
    String punteria  // Calculado: hits/(hits+fallos) * 100
) {}
```

#### 5.6 Eliminar archivos obsoletos
- `PartidasStatsEntity.java` → **Eliminar** (ya no se usa MySQL para stats)
- `PartidasStatsRepository.java` (MySQL) → **Eliminar**

---

### FASE 6 — Eliminar/Refactorizar `BarcoEntity`
**Prioridad: Media | Riesgo: Bajo**

- **Eliminar** `BarcoEntity.java`
- **Eliminar** `BarcoRepository.java`
- Sustituir por `BarcosCatalogoEntity` + `BarcosCatalogoRepository`
- Actualizar `CharacterFactory.java` para que use datos de la tabla `PERSONAJES` y `PERSONAJE_FLOTA` en lugar de tener la configuración hardcodeada

---

### FASE 7 — Actualizar Frontend Angular
**Prioridad: Media | Riesgo: Bajo**

#### 7.1 Actualizar `auth.service.ts`
```diff
 export interface StatsDTO {
-  nickname: string;
-  partidasGanadas: number;
-  impactosAcertados: number;
-  impactosFallados: number;
-  punteria: string;
+  username: string;
+  partidasJugadas: number;
+  partidasGanadas: number;
+  hitsAcertados: number;
+  hitsFallados: number;
+  barcosHundidos: number;
+  punteria: string;
 }
```

#### 7.2 Actualizar `perfil.ts` y `perfil.html`
- Adaptar las referencias de `stats.impactosAcertados` → `stats.hitsAcertados`
- Añadir visualización de `barcosHundidos` y `partidasJugadas`

---

## 3. Justificación del uso de MongoDB para Estadísticas

| Criterio | MySQL (actual) | MongoDB (propuesto) |
|---|---|---|
| **Tipo de dato** | Agregado global por jugador | Granular por partida |
| **Crecimiento** | 1 registro/jugador (fijo) | N registros/jugador (creciente) |
| **Consultas** | Simple: `findByNickname` | Flexibles: agregaciones, rankings, historial |
| **Esquema** | Rígido | Flexible (se pueden añadir campos sin migración) |
| **Rendimiento escritura** | Actualización concurrente (locks) | Inserción append-only (sin conflictos) |
| **Caso de uso** | ACID transaccional | Analytics y alta escritura |

**MongoDB es ideal para estadísticas** porque:
- Cada partida genera un documento independiente (insert, nunca update)
- Las consultas de ranking/historial se hacen con el Aggregation Framework
- Se puede escalar horizontalmente si el volumen crece
- No requiere migración de esquema al añadir nuevos campos de stats

---

## 4. Orden de Ejecución Recomendado

```
FASE 1 → FASE 4 → FASE 2 → FASE 3 → FASE 5 → FASE 6 → FASE 7
```

| Orden | Fase | Motivo |
|---|---|---|
| 1º | **Fase 1** — Config MongoDB | Base necesaria para todo lo demás |
| 2º | **Fase 4** — Entidades nuevas | No rompe nada existente, solo añade |
| 3º | **Fase 2** — UsuarioEntity | Muchos archivos dependen de esto |
| 4º | **Fase 3** — PartidaEntity | Depende de UsuarioEntity actualizada |
| 5º | **Fase 5** — Migrar Stats a Mongo | Depende de las entidades nuevas |
| 6º | **Fase 6** — Eliminar BarcoEntity | Limpieza, bajo riesgo |
| 7º | **Fase 7** — Frontend | Se adapta a los cambios del backend |

---

## 5. Checklist de archivos a modificar/crear/eliminar

### Crear
- [x] `api/config/MongoConfig.java`
- [x] `api/config/JpaConfig.java`
- [x] `api/model/entity/PersonajeEntity.java`
- [x] `api/model/entity/BarcosCatalogoEntity.java`
- [x] `api/model/entity/PersonajeFlotaEntity.java`
- [x] `api/model/entity/PersonajeFlotaId.java`
- [x] `api/model/entity/PartidaStatsDocument.java` (Mongo)
- [ ] `api/model/dto/StatsAgregadasDTO.java`
- [x] `api/repository/mysql/PersonajeRepository.java`
- [x] `api/repository/mysql/BarcosCatalogoRepository.java`
- [x] `api/repository/mysql/PersonajeFlotaRepository.java`

### Modificar
- [x] `application.properties` — Añadir config MongoDB
- [x] `UsuarioEntity.java` — Renombrar campos
- [x] `PartidaEntity.java` — Reestructuración completa
- [x] `EstadoPartida.java` — Añadir valores enum
- [x] `UsuarioRepository.java` — Adaptar métodos
- [ ] `PartidaRepository.java` — Adaptar métodos
- [x] `EstadisticasRepository.java` — Convertir a MongoRepository
- [x] `UsuarioService.java` — Renombramiento de campos
- [x] `IUsuarioService.java` — Adaptar interfaz
- [ ] `EstadisticasService.java` — Migrar a MongoDB
- [ ] `IEstadisticasService.java` — Actualizar interfaz
- [x] `AuthService.java` — Adaptar a nuevos nombres
- [x] `AuthController.java` — Adaptar a nuevos nombres
- [ ] `PartidaController.java` — Adaptar al nuevo esquema
- [x] `GameSocketController.java` — Integrar persistencia partidas
- [x] `LoginDTO.java` — nickname → username
- [x] `RegistroDTO.java` — nickname → username
- [ ] `ActualizarStatsDTO.java` — Reestructurar
- [x] `UpdateNicknameDTO.java` — Adaptar
- [x] `UpdatePasswordDTO.java` — Adaptar
- [x] `UpdateProfilePictureDTO.java` — Adaptar
- [x] **Frontend** `auth.service.ts` — StatsDTO + nombres
- [x] **Frontend** `perfil.ts` — Adaptar stats
- [x] **Frontend** `perfil.html` — Nuevos campos stats

### Eliminar
- [x] `BarcoEntity.java`
- [x] `BarcoRepository.java`
- [x] `PartidasStatsEntity.java`
- [x] `PartidasStatsRepository.java` (MySQL)
