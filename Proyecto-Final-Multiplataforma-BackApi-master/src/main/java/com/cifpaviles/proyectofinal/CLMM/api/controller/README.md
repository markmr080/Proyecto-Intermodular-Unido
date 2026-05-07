# API Controllers (Backend)

Este paquete contiene los controladores que exponen los endpoints internos y de negocio propios del backend de la aplicación.

## Componentes

### `EstadisticasController`
Expone el endpoint de estadísticas agregadas a través de `GET /api/estadisticas/jugador/{username}`. Permite al cliente (Angular) o al Middleware obtener el resumen de victorias, barcos hundidos y porcentaje de acierto de un jugador consultando tanto a MySQL como a MongoDB internamente de manera unificada.

### `PartidaController`
Controller REST para consultar el historial de partidas persistidas en MySQL y su estado.

### `GameSocketController`
Gestiona toda la lógica de WebSocket para las partidas multijugador en tiempo real.
