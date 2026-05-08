# partida-activa

Componente principal de la **pantalla de juego activa** (combate naval).

## Archivos
- [partida-activa.component.html](./partida-activa.component.html) — Estructura visual del tablero, stats de jugadores y modales
- [partida-activa.component.ts](./partida-activa.component.ts) — Lógica del juego, WebSocket, bot de test y targeting de habilidades
- [partida-activa.component.css](./partida-activa.component.css) — Estilos del tablero, panel de habilidades, modal de fin de partida y modo targeting

## Descripción

Gestiona la partida completa desde la **fase de colocación de barcos** hasta el **combate** y el **fin de partida**.

### Funcionalidades principales
- **Fase COLOCACION**: El jugador coloca sus barcos en el tablero propio (horizontal/vertical). La flota se obtiene dinámicamente del personaje asignado por el backend (`miJugador.personaje.flotaComoListaTamanos`).
- **Fase COMBATE**: El jugador ataca celdas del tablero enemigo. Muestra habilidades activas con cooldowns. Las habilidades de área activan un **modo targeting** (banner naranja + cursor crosshair) en el que el siguiente clic en el tablero envía la habilidad con coordenadas.
- **Barcos restantes**: Calculados en tiempo real contando celdas `BARCO` en cada tablero.
- **Modal de fin de partida**: Se activa cuando `juegoActivo === false`. Muestra VICTORIA o DERROTA con animación y botón de salida.
- **Modo Test (Bot)**: Si `localStorage[test_mode_ROOMCODE]` está activo, coloca barcos aleatoriamente respetando espacios mínimos y ataca de forma aleatoria con delay de 1.5 s.

### Referencias
- `SocketService` — comunicación WebSocket (atacar, colocarBarcos, usarHabilidad)
- `GameState` (backend) — estado global que se recibe vía socket en cada acción
- `seleccion-personajes` — guarda el `personajeId` en localStorage antes de navegar aquí
