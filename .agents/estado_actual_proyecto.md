# 📊 Estado Actual del Proyecto — Warhammer Battleship

> **Generado:** 2026-05-07  
> **Fuentes de referencia:** Código fuente real del Backend y Frontend, documentación en `.agents`  
> **Método:** Auditoría cruzada tras la ejecución de la migración de BBDD.

---

## 1. Resumen Ejecutivo

| Área | Estado |
|------|--------|
| **Seguridad JWT + Middleware** | ✅ Implementado y funcional (incluye Fingerprinting) |
| **Gestión de Usuarios (CRUD) y Email** | ✅ Implementado |
| **Configuración BBDD Híbrida (MySQL + Mongo)** | ✅ Completado (`JpaConfig` y `MongoConfig` creados) |
| **Migración de Entidades (nuevo esquema)** | ✅ Completado (todas las fases de `plan_implementacion.md` ejecutadas) |
| **Persistencia del Juego en Tiempo Real** | ✅ Completado (`GameSocketController` guarda en MySQL y Mongo) |
| **Frontend adaptado al nuevo esquema** | ✅ Completado (`auth.service.ts` usa `username`) |
| **Endpoint REST de Estadísticas** | ❌ **Pendiente (Falta Controlador)** |
| **Archivos de Despliegue (Docker)** | ❌ **Pendiente** |

> [!SUCCESS]
> **Gran parte del Plan de Implementación ya ha sido ejecutado.** El código base actual SÍ ha migrado de `nickname` a `username`, y se han creado todas las entidades de MySQL (`PartidaEntity` nueva, `PersonajeEntity`, `BarcosCatalogoEntity`) y el esquema de estadísticas de MongoDB (`PartidaStatsDocument`). 

> [!WARNING]
> **Riesgo Actual:** Aunque el Frontend ya pide los datos a `/api/estadisticas/jugador/{username}` y el `EstadisticasService` existe, **nunca se llegó a crear la clase `EstadisticasController`**, por lo que esa petición está fallando (HTTP 404) en la pantalla de Menú/Perfil.

---

## 2. Auditoría del Código Backend

### ✅ 2.1 Migración de Base de Datos Híbrida
Se ha ejecutado la reestructuración completa:
- `application.properties` está configurado con las URI de MySQL y MongoDB.
- Se han segregado las configuraciones en `api.config.JpaConfig` y `api.config.MongoConfig`.

### ✅ 2.2 Entidades e Interfaces
- `UsuarioEntity`: Ahora usa `username` y `passwordHash`.
- `PartidaEntity`: Relaciona lógicamente con el Host y el Ganador (`UsuarioEntity`), con Fechas de inicio/fin, eliminando los campos de stats ensuciados en MySQL.
- Se crearon las entidades del core del juego: `PersonajeEntity`, `BarcosCatalogoEntity`, `PersonajeFlotaEntity`.
- **MongoDB**: Existe la entidad documental `PartidaStatsDocument` y su repositorio `EstadisticasRepository`.

### ✅ 2.3 Sockets y Persistencia del Motor
En `GameSocketController`, los eventos `colocar-barcos` y `atacar` se han modificado para:
- Crear una instancia en `PARTIDAS` (MySQL) llamando a `iniciarPartidaBD()`.
- Al finalizar el juego, actualizar la tabla `PARTIDAS` declarando al ganador, e invocar a MongoDB para guardar 2 documentos (uno por cada jugador) con sus métricas exactas (`guardarStatsJugador()`).

### ❌ 2.4 Controlador de Estadísticas (Faltante Crítico)
El servicio `EstadisticasService` calcula `StatsAgregadasDTO` sumando todas las estadísticas de Mongo y las victorias de MySQL de manera perfecta. Sin embargo, no hay ningún punto de acceso REST para que Angular obtenga esa información. **Se requiere crear `EstadisticasController`**.

---

## 3. Estado del Frontend

| Componente | Estado tras migración |
|---|---|
| Interfaz `StatsDTO` | ✅ Refactorizada para usar `username` y `hitsAcertados`. |
| Login / Register / Update | ✅ Refactorizados para mandar JSONs con la propiedad `username`. |
| Petición HTTP Estadísticas | ⚠️ Se hace a `http://localhost:8080/api/estadisticas/jugador/{username}`, pero el backend devuelve 404. |

---

## 4. Arquitectura de Despliegue en Producción

Se ha validado la estrategia de despliegue según el archivo `.agents/despliegue_produccion.md`.

| Infraestructura | Decisión Final |
|---|---|
| **Servidor Host** | Ubuntu Server en Microsoft Azure |
| **Orquestación** | Docker Compose |
| **Bases de Datos** | Contenedores locales MySQL 8 y MongoDB oficial en el propio Ubuntu (Aislados). Se descarta el uso de MongoDB Atlas. |
| **Backend** | Spring Boot `.jar` contenerizado (`backend-api`) |
| **Frontend** | Contenedor Nginx (`frontend-web`) sirviendo estáticos de Angular y actuando de Proxy Inverso para solventar CORS. |

---

## 5. Próximos Pasos Recomendados (Prioridades)

| Prioridad | Acción | Detalle |
|---|---|---|
| 🔴 **URGENTE** | **Crear `EstadisticasController`** | En el paquete `api.controller`. Exponer un método GET que devuelva el objeto `StatsAgregadasDTO` pidiéndoselo al `EstadisticasService`. |
| 🟡 Media | **Archivos Docker** | Crear `Dockerfile` para Backend, `Dockerfile` para Nginx y el `docker-compose.yml` base. |
| 🟡 Media | **Configurar Variables** | Migrar `application.properties` a `application-prod.properties` para enmascarar contraseñas. |
| 🟢 Baja | **Refinar Semillas** | Insertar datos predeterminados en `Personajes` y `BarcosCatalogo` usando un archivo `data.sql` si fuera necesario para producción. |
