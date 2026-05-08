# 🌌 Warhammer Battleship - Proyecto Intermodular (2º DAM)

Este archivo centraliza el contexto, la arquitectura y las reglas de desarrollo del proyecto para los agentes de IA. 

## 🏗️ Arquitectura Detallada (Flujo de Datos)

El sistema opera bajo una arquitectura de **Aislamiento Perimetral**:

1.  **Capa de Cliente (Angular 17+):** 
    *   Gestiona la interfaz de usuario y la lógica de presentación.
    *   Mantiene una conexión persistente vía **Socket.IO** para el juego en tiempo real.
    *   Inyecta el `X-Fingerprint` en cada petición HTTP/WS.

2.  **Capa Middleware (Escudo de Seguridad):**
    *   **Autenticación en 2 Fases:**
        *   *Fase 1:* Login inicial con cuenta de servicio (`middleware_admin`) para obtener túnel seguro.
        *   *Fase 2:* Validación de credenciales reales contra la BBDD.
    *   **JwtFilter:** Valida la firma del token y la coincidencia del Fingerprint.
    *   **LobbyManager:** Gestiona las salas de espera en memoria antes de pasar al motor de juego.

3.  **Capa API (Lógica de Negocio y Motor):**
    *   **GameEngine:** Gestiona el estado de la partida (tableros, turnos, habilidades).
    *   **Servicios de Persistencia:** Orquestan el guardado en las dos bases de datos.

4.  **Capa de Datos (Persistencia Políglota):**
    *   **MySQL:** Almacena usuarios, catálogo de barcos y el estado final de las partidas (ACID).
    *   **MongoDB Atlas:** Almacena el desglose detallado de cada acción para analíticas (Estadísticas).

---

## 📜 Instrucciones de Desarrollo (Extraído de .agents)

1.  **Simplicidad:** Programa métodos que hagan una sola cosa. Código claro y mantenible.
2.  **No Inventar:** Nunca implementes funcionalidades no pedidas expresamente.
3.  **Documentación Local:** Añade un `README.md` en cada carpeta de componente nuevo con enlaces a sus archivos (HTML, TS, CSS) y una breve descripción.
4.  **Verificación:** Antes de crear algo, comprueba si ya existe en el código o en la carpeta `.agents`.
5.  **Comentarios:** Explica el "por qué" y las referencias a otras partes del código.
6.  **Estado Crítico:** El proyecto actualmente tiene pendiente el `EstadisticasController` (HTTP 404 en el Front).

---

## 🛠️ Best Practices

### ☕ Java & Spring Boot
- **Records:** Usar `record` para DTOs y objetos inmutables (Java 20+).
- **Inyección:** Preferir inyección por constructor o `@RequiredArgsConstructor` de Lombok.
- **DTOs:** Nunca exponer entidades JPA directamente en los controladores.
- **Optional:** Evitar retornos `null`, usar `Optional<T>`.
- **Excepciones:** Usar un `@ControllerAdvice` para el manejo global de errores.
- **Streams:** Utilizar la API de Streams para manipulación de colecciones.
- **Lombok:** Usar `@Data`, `@Builder` y `@Slf4j` para mantener el código limpio.

### 🔷 TypeScript & Angular
- **Tipado:** Uso estricto de tipos. Evitar `any`, preferir `unknown`.
- **Standalone:** Usar componentes standalone (default en Angular 17+).
- **Signals:** Preferir Signals para el estado del componente y reactividad.
- **Control Flow:** Usar sintaxis nativa (`@if`, `@for`) en lugar de directivas legacy.
- **Performance:** `changeDetection: ChangeDetectionStrategy.OnPush`.

---

## 📊 Estado del Proyecto
| Módulo | Estado |
| :--- | :--- |
| **Seguridad JWT** | ✅ Operativo |
| **BBDD Híbrida** | ✅ Configurada (MySQL + Mongo) |
| **Motor de Juego** | ✅ Funcional en tiempo real |
| **Estadísticas** | ❌ Falta `EstadisticasController` |
| **Dockerización** | ❌ Pendiente |

---

> [!TIP]
> Para más detalles sobre endpoints o flujos específicos, consulta la carpeta [Arquitectura](Proyecto-Intermodular-Unido/.agents/Arquitectura).
