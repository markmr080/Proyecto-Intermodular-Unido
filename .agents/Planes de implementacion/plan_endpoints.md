# Plan de Implementación de Endpoints

Este documento detalla los pasos a seguir para que el backend exponga el 100% de los endpoints requeridos por el frontend (según lo descrito en `.agents/endpoints.md`).

## 1. Análisis de Estado Actual

Tras revisar el código fuente de los controladores, se obtiene el siguiente diagnóstico:

### 1.1 Endpoints de Autenticación (`/api/auth`)
✅ **Estado:** Completamente implementado.
- `AuthController.java` expone todos los POST necesarios (`login`, `validate-user`, `register`, `update-*`, etc.).
- Coincide 1 a 1 con lo que pide `AuthService.ts` en Angular.

### 1.2 Endpoints de Lobby (`/api/lobby`)
✅ **Estado:** Completamente implementado.
- `LobbyController.java` expone GET, POST y DELETE.
- Coincide con lo que invoca `RoomService.ts` en Angular.

### 1.3 Eventos de WebSocket (Puerto 8081)
✅ **Estado:** Completamente implementado.
- `GameSocketController.java` y `SocketService.java` de backend tienen definidos todos los listeners (`solicitar-unirse`, `atacar`, `colocar-barcos`, etc.).

### 1.4 Endpoints de Estadísticas (`/api/estadisticas`)
❌ **Estado:** Faltante (0%).
- Actualmente el frontend lanza una petición `GET http://localhost:8080/api/estadisticas/jugador/{username}` en la pantalla del Menú para dibujar la puntería, los impactos acertados y las partidas ganadas.
- En el backend **no existe** un `EstadisticasController` que atienda esa ruta.

---

## 2. Fases de Implementación (Solución)

El único bloque pendiente es el de Estadísticas (MongoDB). Para completarlo, se seguirá este flujo de desarrollo:

### Fase 1: Creación del DTO de Transferencia
Se debe crear un objeto que la API enviará como JSON al cliente, haciendo "match" directo con la interfaz `StatsDTO` de Angular.

**Archivo a crear:** `api/model/dto/StatsAgregadasDTO.java`
**Estructura esperada:**
```java
public record StatsAgregadasDTO(
    String username,
    int partidasJugadas,
    int partidasGanadas,
    int hitsAcertados,
    int hitsFallados,
    int barcosHundidos,
    String punteria // Calculado como (acertados / totales) + "%"
) {}
```

### Fase 2: Lógica en el Servicio (MongoDB)
Hay que dotar al servicio actual de la capacidad de buscar todos los documentos de un usuario en MongoDB y sumarlos.

**Archivos a modificar:** `IEstadisticasService.java` y `EstadisticasService.java`
**Flujo:**
1. Obtener la lista de `PartidaStatsDocument` mediante `findByUsername(username)`.
2. Sumar acumulativamente todos los `hitsAcertados`, `hitsFallados` y `barcosHundidos`.
3. Consultar a `PartidaRepository` (MySQL) cuántas partidas tiene como `ganador_id` para el campo `partidasGanadas`.
4. Devolver la instancia de `StatsAgregadasDTO`.

### Fase 3: Creación del Controlador
Exponer el endpoint protegido para que Angular lo consuma.

**Archivo a crear:** `api/controller/EstadisticasController.java`
**Estructura esperada:**
```java
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {
    
    // Inyección de EstadisticasService
    
    @GetMapping("/jugador/{username}")
    public ResponseEntity<StatsAgregadasDTO> getStatsJugador(@PathVariable String username) {
         // Llamar a servicio y retornar HTTP 200 OK
    }
}
```

---

## 3. Checklist de Ejecución

- [ ] 1. Crear `StatsAgregadasDTO.java`.
- [ ] 2. Actualizar el repositorio `PartidaRepository.java` para poder buscar el conteo de victorias por usuario.
- [ ] 3. Desarrollar el cálculo lógico en `EstadisticasService.java`.
- [ ] 4. Crear el `EstadisticasController.java` y levantar el endpoint.
- [ ] 5. Comprobar que Spring Security no bloquea la petición (o que está autorizada correctamente con JWT).
