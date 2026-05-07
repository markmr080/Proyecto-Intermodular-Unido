# Plan de Implementación: Corrección de Errores de Flujo de Juego

Este documento detalla la investigación y las correcciones propuestas para resolver los tres problemas reportados en `.agents/Log de errores/errores a solucionar.md`.

## User Review Required
Por favor, revisa las soluciones planteadas y confirma si estás de acuerdo para que proceda con su implementación.

## 1. Problema: Imágenes de personajes rotas en selección
**Causa:** Las URLs que se están usando en `uiMetadata` (en `seleccion-personajes.component.ts`) apuntan a sitios como Reddit o Wikia que bloquean el *hotlinking* (CORS y Error 403 Forbidden). Esto hace que el navegador se niegue a cargar las imágenes.
**Solución Propuesta:** 
- Reemplazar las URLs de imágenes en `uiMetadata` por URLs confiables del API de `dicebear` (el mismo que ya usamos para los avatares) para garantizar que siempre carguen, generando un aspecto consistente para los personajes.

## 2. Problema: El juego se queda en la pantalla de carga
**Causa:** El backend envía correctamente el estado de la partida (`gameState`) cuando el jugador hace `joinRoom`. Sin embargo, la librería de `socket.io-client` ejecuta sus callbacks **fuera de la "Angular Zone"**. Por tanto, la variable `this.gameState` se actualiza internamente, pero la vista HTML de Angular no se entera y sigue mostrando el *spinner* de carga (porque el `*ngIf="gameState"` no se reevalúa).
**Solución Propuesta:** 
- Inyectar `ChangeDetectorRef` en `partida-activa.component.ts`.
- Llamar a `this.cdr.detectChanges()` dentro del `subscribe` de `gameState$` para forzar la actualización de la interfaz gráfica de forma segura en cuanto llega la información del servidor.

## 3. Problema: Los jugadores no vuelven a la sala al finalizar la partida
**Causa:** Cuando el backend detecta que las vidas de un jugador llegan a 0, cambia `juegoActivo` a `false` y emite un mensaje de victoria ("¡FIN DE LA PARTIDA!"). Sin embargo, el frontend actualiza el texto pero no ejecuta ninguna acción de navegación para devolver al usuario al menú principal.
**Solución Propuesta:** 
- Modificar el archivo `partida-activa.component.ts` para que, al recibir un nuevo `gameState`, evalúe si `state.juegoActivo === false`.
- Si la partida ha terminado, mostrar el ganador en pantalla y, tras un *delay* de 4 segundos (usando `setTimeout`), redirigir a los jugadores automáticamente a la pantalla de salas usando el `Router` (`this.router.navigate(['/lista-salas'])`).

## Verification Plan
1. Iniciar sesión con un usuario, crear una sala e invitar a un segundo usuario.
2. Confirmar que en la pantalla de selección ambos ven imágenes de los personajes correctamente.
3. Al seleccionar y empezar partida, confirmar que los dos usuarios saltan instantáneamente al tablero sin quedarse en carga.
4. Simular que se hunden todos los barcos de un jugador para confirmar la visualización de "FIN DE PARTIDA" y la posterior redirección automática.
