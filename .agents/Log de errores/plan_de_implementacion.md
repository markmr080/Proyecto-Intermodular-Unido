# 🚀 Plan de Implementación — Corrección de Errores

Este documento detalla la estrategia y las tareas necesarias para resolver los errores pendientes identificados en el [Log de errores](./errores%20a%20solucionar.md).

---

## 📅 Fases de Ejecución

### Fase 1: Seguridad y Autenticación (Crítico)
*Objetivo: Cerrar brechas de seguridad en WebSockets y mejorar la gestión de sesiones.*

1.  **Seguridad WebSockets (JWT):** Implementar validación de JWT en el handshake de Socket.IO.
2.  **Password Reset:** Modificar el flujo de recuperación de contraseña para no exponer el JWT en la URL.
3.  **Gestión de Ciclo de Vida del Token:** Asegurar el borrado del token en `localStorage`/`sessionStorage` al cerrar pestaña o salir.
4.  **Guardias de Navegación:** Implementar/Reforzar `AuthGuard` para redirigir al login si no hay token.

### Fase 2: Lógica de Juego y Lobby
*Objetivo: Corregir bugs de sincronización en salas y terminar mecánicas de personajes.*

1.  **Sincronización de Solicitudes:** Actualizar el estado de "pendiente" a "rechazado/cancelado" para otros jugadores cuando el admin acepta a uno.
2.  **Orden de Salas:** Modificar el backend para devolver las salas ordenadas por fecha de creación (DESC).
3.  **Habilidades de Personajes:**
    *   Terminar implementación de **Ikit Claw**.
    *   Corregir "Disparo de Saloma" y frecuencia de la pasiva de **Aranessa**.
4.  **Botón Abandonar:** Validar que el evento de "abandonar" limpie correctamente los estados en todos los clientes conectados.

### Fase 3: UI/UX y Diseño Responsive
*Objetivo: Pulir la interfaz para una experiencia premium en todos los dispositivos.*

1.  **Navegación Dinámica:** Ocultar la `navbar` en selección de personaje y durante la partida.
2.  **Correcciones de Layout:**
    *   Ajustar visibilidad del botón "Jugar" (>768px).
    *   Corregir desbordamiento de textos de habilidades.
    *   Reposicionar botón de salir en móvil.
3.  **Ajustes Estéticos:**
    *   Quitar funcionalidad de botón al logo.
    *   Corregir solapamiento del logo en resoluciones intermedias.
    *   Bloqueo de scroll global y uso de scroll interno en contenedores.

### Fase 4: Alineación Arquitectónica
*Objetivo: Mantener la consistencia del middleware.*

1.  **Migración de Controladores:** Mover `PersonajesController` y `EstadisticasController` al paquete `middleware` para asegurar que pasan por los filtros correctos.

---

## 📊 Tabla de Tareas

| ID | Tarea | Prioridad | Complejidad | Estado |
|:---|:---|:---:|:---:|:---:|
| SEC-01 | Seguridad JWT en WebSockets | 🔥 Alta | Media | ⏳ Pendiente |
| SEC-02 | Reset Password vía POST (no URL) | 🔥 Alta | Baja | ⏳ Pendiente |
| LOB-01 | Sincronización de solicitudes de sala | ⚡ Media | Media | ⏳ Pendiente |
| LOB-02 | Ordenación de salas por fecha | ⚡ Media | Baja | ⏳ Pendiente |
| GAME-01 | Finalizar Ikit Claw | ⚡ Media | Media | ⏳ Pendiente |
| GAME-02 | Fix Habilidades Aranessa | ⚡ Media | Media | ⏳ Pendiente |
| UI-01 | Ocultar Navbar en Juego/Selección | 🟢 Baja | Baja | ⏳ Pendiente |
| UI-02 | Fix Responsive (Botones, Overflows) | 🟢 Baja | Media | ⏳ Pendiente |
| ARCH-01 | Mover controladores al Middleware | 🟢 Baja | Baja | ⏳ Pendiente |

---

## 🛠️ Herramientas y Archivos Clave

- **Backend:** `SecurityConfig.java`, `SocketIOConfig.java`, `GameEngine.java`, `LobbyService.java`.
- **Frontend:** `auth.service.ts`, `game.socket.service.ts`, `navbar.component.ts`, `partida-activa.component.css`.

> [!NOTE]
> Se recomienda empezar por la **Fase 1** debido a las implicaciones de seguridad, seguido de la **Fase 2** para estabilizar la jugabilidad antes de los ajustes visuales.
