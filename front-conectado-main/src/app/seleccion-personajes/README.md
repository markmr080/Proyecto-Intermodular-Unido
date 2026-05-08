# seleccion-personajes

Componente de la **pantalla de selección de personaje** antes de iniciar una partida.

## Archivos
- [seleccion-personajes.component.html](./seleccion-personajes.component.html) — Carrusel de personajes con imagen, nombre, barcos y habilidades
- [seleccion-personajes.component.ts](./seleccion-personajes.component.ts) — Lógica de selección, modo test y navegación
- [seleccion-personajes.component.css](./seleccion-personajes.component.css) — Estilos del carrusel y tarjetas de personaje

## Descripción

Permite a los jugadores elegir su personaje antes de la partida. Cada personaje tiene una **flota única** de barcos y **habilidades exclusivas**.

### Funcionalidades principales
- **Carrusel de personajes**: navega entre Wulfrik, Aislinn, Lokhir y Aranessa.
- **Flotas únicas**: cada personaje muestra sus barcos reales (coinciden con la BD del backend):
  - Wulfrik → 5, 4, 3, 3, 2
  - Aislinn → 5, 4, 3, 2, 2
  - Lokhir  → 5, 3, 3, 2, 2
  - Aranessa → 4, 4, 3, 3, 2
- **Seleccionar**: guarda el `tipo` del personaje en `localStorage[personaje_ROOMCODE]` para que `partida-activa` lo envíe en `join-room`.
- **Test Partida**: activa el modo test (`localStorage[test_mode_ROOMCODE]`), asigna personaje aleatorio al J2 y navega directamente al combate.

### Referencias
- `SocketService.seleccionarPersonaje()` — notifica al otro jugador de la selección
- `partida-activa` — lee el `personajeId` del localStorage al entrar
- `DataInitializer.java` (backend) — define las flotas reales que se deben reflejar aquí
