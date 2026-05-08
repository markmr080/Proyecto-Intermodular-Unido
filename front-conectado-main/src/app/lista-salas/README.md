# lista-salas

Componente de la **pantalla de lista de salas disponibles** para unirse a una partida multijugador.

## Archivos
- [lista-salas.html](./lista-salas.html) — Lista de salas, botones de acción y modal de espera
- [lista-salas.ts](./lista-salas.ts) — Lógica de carga de salas, unirse y gestión del modal
- [lista-salas.css](./lista-salas.css) — Estilos de la lista, tarjetas de sala y modal temático

## Descripción

Muestra las salas en estado `ESPERANDO` y permite crear una nueva o unirse a una existente.

### Funcionalidades principales
- **Lista de salas**: se refresca automáticamente cada 10 s y muestra solo salas en estado `ESPERANDO`.
- **Buscar sala**: filtro por código de sala en tiempo real.
- **Crear sala**: genera un código único y navega a la pantalla de selección de personaje.
- **Unirse**: envía solicitud al anfitrión vía `SocketService.solicitarUnirse()`.
- **Modal "Esperando al anfitrión"**: aparece al pulsar "Unirse" mientras se espera respuesta del anfitrión. Incluye loader animado de tres puntos y botón de cancelar. Se cierra automáticamente al aceptar/rechazar la solicitud.
- **Cancelar solicitud**: cierra el modal localmente sin necesidad de notificar al servidor.

### Referencias
- `SocketService.solicitudAceptada$` — navega a la partida si el anfitrión acepta
- `SocketService.solicitudRechazada$` — muestra alerta y cierra el modal si el anfitrión rechaza
- `RoomService.getRooms()` — obtiene la lista de salas del backend REST
