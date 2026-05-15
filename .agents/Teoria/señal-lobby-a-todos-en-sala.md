# Patrón: Señal desde el Frontend → Middleware → Broadcast a toda la sala

Este documento explica cómo implementar el patrón en el que el frontend emite una señal por socket, el middleware (Lobby) la procesa y la reenvía a **todos los participantes de una sala**.

---

## Por qué el Lobby no usa `getRoomOperations()`

En el `GameSocketController` se puede usar `server.getRoomOperations(roomCode).sendEvent(...)` porque los clientes hacen `client.joinRoom(roomCode)` explícitamente.

En el **Lobby** (`SocketService.java`) eso no ocurre. En su lugar existe el mapa `userSockets` (`Map<String, UUID>`) que asocia cada userId con su socketId. El patrón correcto es:

1. Buscar la sala en `lobbyManager` para obtener los IDs de J1 y J2.
2. Localizar sus sockets en `userSockets`.
3. Enviarles el evento individualmente.

Este es exactamente el mismo patrón que usan `iniciar-partida`, `seleccionar-personaje` y `comenzar-juego` en el código actual.

---

## Cambio 1 — Backend: `SocketService.java`

**Archivo:** `middleware/sockets/SocketService.java`

Dentro del método `registrarListeners()`, añade al final (antes del último `}`):

```java
server.addEventListener("mi-señal-lobby", Map.class, (cliente, data, ackRequest) -> {
    String codigoSala = (String) data.get("codigoSala");
    String userId     = (String) data.get("userId");
    String mensaje    = (String) data.get("mensaje");

    Optional<LobbyRoom> partidaOpt = lobbyManager.getRoom(codigoSala);
    if (partidaOpt.isPresent()) {
        LobbyRoom partida = partidaOpt.get();

        // Construir el payload de respuesta
        Map<String, String> respuesta = new java.util.HashMap<>();
        respuesta.put("codigoSala", codigoSala);
        respuesta.put("de", userId);
        respuesta.put("mensaje", mensaje);

        // Enviar a J1
        UUID j1Socket = userSockets.get(partida.getJugador1());
        if (j1Socket != null) {
            server.getClient(j1Socket).sendEvent("notificacion-sala", respuesta);
        }

        // Enviar a J2 (si existe)
        String j2Id = partida.getJugador2();
        UUID j2Socket = (j2Id != null) ? userSockets.get(j2Id) : null;
        if (j2Socket != null) {
            server.getClient(j2Socket).sendEvent("notificacion-sala", respuesta);
        }
    }
});
```

> **Nota:** `userSockets` es un atributo privado de la clase `SocketService`. El nuevo listener va **dentro de la misma clase** en `registrarListeners()`, por lo que tiene acceso directo sin necesidad de pasarlo como parámetro.

---

## Cambio 2 — Frontend: `socket.service.ts`

**Archivo:** `front-conectado-main/src/app/services/socket.service.ts`

```typescript
// 1. Añadir el Subject en la sección "--- Subjects para Lobby ---" (~línea 20)
public notificacionSala$ = new Subject<any>();

// 2. Añadir el listener dentro del método connect(), en el bloque de Lobby (~línea 75)
this.socket.on('notificacion-sala', (data: any) =>
  this.ngZone.run(() => this.notificacionSala$.next(data))
);

// 3. Añadir el método emit público
public enviarSenalSala(codigoSala: string, userId: string, mensaje: string) {
  this.socket.emit('mi-señal-lobby', { codigoSala, userId, mensaje });
}
```

---

## Cambio 3 — Frontend: Componente (p. ej. `partida.component.ts`)

```typescript
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

// Dentro de la clase del componente:
private destroy$ = new Subject<void>();
mensajeSala: string = '';

ngOnInit(): void {
  // Suscribirse a las notificaciones de sala
  this.socketService.notificacionSala$
    .pipe(takeUntil(this.destroy$))
    .subscribe((data) => {
      console.log('Notificación recibida de', data.de, ':', data.mensaje);
      this.mensajeSala = data.mensaje; // actualizar la UI
    });
}

ngOnDestroy(): void {
  this.destroy$.next();
  this.destroy$.complete();
}

// Llamar desde un botón o desde código
enviarNotificacion(): void {
  this.socketService.enviarSenalSala(
    this.codigoSala,
    this.currentUser.username,
    'Mensaje para todos en la sala'
  );
}
```

```html
<!-- En el template HTML -->
<button (click)="enviarNotificacion()">Enviar señal</button>
<p *ngIf="mensajeSala">{{ mensajeSala }}</p>
```

---

## Flujo completo

```
Componente Angular
  └─► enviarNotificacion()
        └─► socketService.enviarSenalSala(...)
              └─► socket.emit('mi-señal-lobby', { codigoSala, userId, mensaje })
                        │
                        ▼ WebSocket puerto 8081
                  SocketService.java (Middleware)
                    ├─► lobbyManager.getRoom(codigoSala)
                    ├─► userSockets.get(j1Id) → socket de J1
                    ├─► sendEvent('notificacion-sala') ──► J1
                    ├─► userSockets.get(j2Id) → socket de J2
                    └─► sendEvent('notificacion-sala') ──► J2
                                │
                                ▼
                  socket.service.ts (Frontend de J1 y J2)
                    └─► notificacionSala$.next(data)
                              │
                              ▼
                  Componente (suscripción activa)
                    └─► actualiza mensajeSala en la UI
```

---

## Checklist de verificación

- [ ] El nombre del evento emitido (`'mi-señal-lobby'`) coincide exactamente en Java y TypeScript
- [ ] El nombre del evento de respuesta (`'notificacion-sala'`) coincide exactamente en Java y TypeScript
- [ ] El nuevo `addEventListener` está dentro del método `registrarListeners()` de `SocketService.java`
- [ ] El listener de `socket.on(...)` está dentro del método `connect()` de `socket.service.ts`
- [ ] Se usa `this.ngZone.run(...)` para que Angular detecte el cambio
- [ ] La suscripción usa `takeUntil(this.destroy$)` para evitar memory leaks
- [ ] `ngOnDestroy()` llama a `this.destroy$.next()` y `this.destroy$.complete()`

---

## Variantes útiles

### Enviar solo al emisor (no al otro)
```java
// En lugar de enviar a J1 y J2, enviar solo al cliente que disparó el evento
cliente.sendEvent("notificacion-sala", respuesta);
```

### Enviar a un jugador específico (no al emisor)
```java
// Enviar solo al rival (el que NO envió la señal)
String rivalId = userId.equals(partida.getJugador1()) 
    ? partida.getJugador2() 
    : partida.getJugador1();
UUID rivalSocket = userSockets.get(rivalId);
if (rivalSocket != null) {
    server.getClient(rivalSocket).sendEvent("notificacion-sala", respuesta);
}
```
