# Guía: Cómo Implementar un Nuevo Evento Socket.IO

Esta guía explica paso a paso cómo añadir un nuevo evento de comunicación bidireccional (cliente ↔ servidor) al proyecto. El stack usa **Socket.IO** con la librería `netty-socketio` en el backend (Java/Spring Boot) y `socket.io-client` en el frontend (Angular).

---

## Arquitectura General

```
Angular (Componente)
       │
       ▼
SocketService.ts         ← único punto de contacto con el socket
  │  socket.emit(...)    ← enviar evento al servidor
  │  Subject$.next(...)  ← recibir evento del servidor
       │
       ▼ WebSocket (puerto 8081)
       │
  ┌────┴──────────────────────────┐
  │  BACKEND (Spring Boot)        │
  │                               │
  │  SocketService.java           │  ← Eventos de LOBBY
  │  GameSocketController.java    │  ← Eventos de JUEGO ACTIVO
  └───────────────────────────────┘
```

> **IMPORTANTE:** Hay **dos controladores de socket** en el backend según la fase del juego:
> - **`SocketService.java`** → Gestiona eventos del **Lobby** (sala de espera, solicitudes, personajes).
> - **`GameSocketController.java`** → Gestiona eventos del **Juego activo** (atacar, habilidades, tablero).
>
> Elige el fichero correcto según en qué fase se usará el nuevo evento.

---

## Paso 1: Backend — Registrar el nuevo evento

### Opción A: Evento de Lobby (`SocketService.java`)

**Archivo:** `Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/middleware/sockets/SocketService.java`

Dentro del método `registrarListeners()`, añade un nuevo `server.addEventListener(...)`:

```java
// Ejemplo: evento "mi-nuevo-evento"
server.addEventListener("mi-nuevo-evento", Map.class, (cliente, data, ackRequest) -> {
    // 1. Leer los datos que envió el cliente
    String codigoSala = (String) data.get("codigoSala");
    String userId     = (String) data.get("userId");

    // 2. Tu lógica de negocio aquí
    System.out.println("Recibido mi-nuevo-evento de " + userId + " en sala " + codigoSala);

    // 3a. Responder SOLO al cliente que lo envió
    cliente.sendEvent("respuesta-evento", "OK");

    // 3b. O responder a TODOS en una sala
    // server.getRoomOperations(codigoSala).sendEvent("respuesta-evento", "OK");

    // 3c. O responder a un jugador específico por su userId
    // UUID targetSocket = userSockets.get(userId);
    // if (targetSocket != null) {
    //     server.getClient(targetSocket).sendEvent("respuesta-evento", "OK");
    // }
});
```

**Patrón de datos:** Si el evento recibe un objeto con múltiples campos, usa `Map.class`. Si recibe un simple `String`, usa `String.class`.

---

### Opción B: Evento de Juego (`GameSocketController.java`)

**Archivo:** `Proyecto-Final-Multiplataforma-BackApi-master/src/main/java/com/cifpaviles/proyectofinal/CLMM/api/controller/GameSocketController.java`

Añade un nuevo método anotado con `@OnEvent` **y** su DTO correspondiente:

```java
// 1. Crear el DTO (clase estática interna al final del fichero, antes del último '}'  )
public static class MiNuevoMessage {
    private String jugadorId;
    private String roomCode;
    private String miCampo;

    public String getJugadorId() { return jugadorId; }
    public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getMiCampo() { return miCampo; }
    public void setMiCampo(String miCampo) { this.miCampo = miCampo; }
}

// 2. Añadir el handler del evento (dentro de la clase GameSocketController)
@OnEvent("mi-nuevo-evento")
public void onMiNuevoEvento(SocketIOClient client, MiNuevoMessage mensaje, AckRequest ackSender) {
    GameEngine engine = roomManager.getRoom(mensaje.getRoomCode());
    if (engine != null && engine.getState() != null) {
        // Tu lógica aquí (modifica el GameState si es necesario)
        System.out.println("mi-nuevo-evento recibido de " + mensaje.getJugadorId());

        // Difundir el nuevo estado a todos en la sala
        difundirEstado(mensaje.getRoomCode(), engine.getState());
    }
}
```

> **Nota:** Los eventos del `GameSocketController` usan DTOs tipados (clases estáticas internas).
> Los del `SocketService` del Lobby usan `Map.class` directamente. Esta es la convención del proyecto.

---

## Paso 2: Frontend — Exponer el evento en `SocketService.ts`

**Archivo:** `front-conectado-main/src/app/services/socket.service.ts`

### 2a. Declarar el Subject (para eventos entrantes del servidor)

Añade un nuevo `Subject` en la sección de propiedades:

```typescript
// En la sección "--- Subjects para Lobby ---" o "--- Subjects para Juego ---"
public miNuevoEvento$ = new Subject<any>(); // Cambia 'any' por tu tipo si lo tienes definido
```

### 2b. Registrar el listener en el método `connect()`

Dentro del bloque de listeners (el método `connect()`), añade:

```typescript
// En el bloque "--- Listeners de Lobby ---" o "--- Listeners de Juego ---"
this.socket.on('respuesta-evento', (data: any) =>
  this.ngZone.run(() => this.miNuevoEvento$.next(data))
);
```

> **IMPORTANTE:** Siempre envuelve la respuesta en `this.ngZone.run(() => ...)`.
> Esto garantiza que Angular detecte el cambio de estado y actualice la vista,
> ya que Socket.IO opera fuera de la zona de Angular.

### 2c. Añadir el método emit (para enviar datos al servidor)

Crea un método público que encapsule el `emit`:

```typescript
// Ejemplo: emitir el nuevo evento
public enviarMiEvento(jugadorId: string, roomCode: string, miCampo: string) {
  this.socket.emit('mi-nuevo-evento', { jugadorId, roomCode, miCampo });
}
```

---

## Paso 3: Frontend — Consumir el evento en el Componente Angular

### 3a. Suscribirse al Subject

```typescript
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

// Dentro de la clase del componente:
private destroy$ = new Subject<void>();

ngOnInit(): void {
  // Suscribirse al nuevo evento
  this.socketService.miNuevoEvento$
    .pipe(takeUntil(this.destroy$))  // Evitar memory leaks
    .subscribe((data) => {
      console.log('Recibido miNuevoEvento:', data);
      // Actualizar el estado del componente aquí
      this.miDato = data;
    });
}

ngOnDestroy(): void {
  this.destroy$.next();
  this.destroy$.complete();
}
```

### 3b. Emitir el evento desde el template o desde código

```typescript
// Llamar al método del servicio cuando quieras enviar el evento
enviarAccion(): void {
  this.socketService.enviarMiEvento(
    this.jugadorId,
    this.roomCode,
    'valor-del-campo'
  );
}
```

```html
<!-- En el template HTML -->
<button (click)="enviarAccion()">Ejecutar Acción</button>
```

---

## Resumen: Los 3 Archivos que Siempre Modificas

| # | Archivo | Qué añades |
|---|---------|------------|
| 1 | `SocketService.java` o `GameSocketController.java` | Handler con la lógica backend |
| 2 | `socket.service.ts` | `Subject` + listener en `connect()` + método `emit` público |
| 3 | `mi-componente.component.ts` | Suscripción al `Subject$` en `ngOnInit` + llamada al emit |

---

## Checklist de Verificación

- [ ] El nombre del evento es **idéntico** en backend y frontend (distingue mayúsculas/minúsculas)
- [ ] El listener en `socket.service.ts` está **dentro del método `connect()`**
- [ ] El Subject está envuelto en `this.ngZone.run()`
- [ ] La suscripción usa `takeUntil(this.destroy$)` para evitar memory leaks
- [ ] El backend responde al cliente correcto (`.sendEvent` individual vs `.getRoomOperations().sendEvent` para todos)
- [ ] Si el evento es de juego, el DTO tiene todos los campos con sus getters/setters

---

## Ejemplo Completo: Evento "chat-mensaje"

A modo de referencia, aquí el flujo completo para un hipotético evento de chat en el Lobby.

### Backend (`SocketService.java`)
```java
server.addEventListener("chat-mensaje", Map.class, (cliente, data, ackRequest) -> {
    String codigoSala = (String) data.get("codigoSala");
    // Difundir a toda la sala
    server.getRoomOperations(codigoSala).sendEvent("chat-recibido", data);
});
```

### Frontend — Servicio (`socket.service.ts`)
```typescript
// Propiedad
public chatRecibido$ = new Subject<{ texto: string; userId: string }>();

// En connect():
this.socket.on('chat-recibido', (data: any) =>
  this.ngZone.run(() => this.chatRecibido$.next(data))
);

// Método emit
public enviarChat(codigoSala: string, texto: string) {
  this.socket.emit('chat-mensaje', { codigoSala, texto });
}
```

### Frontend — Componente
```typescript
this.socketService.chatRecibido$
  .pipe(takeUntil(this.destroy$))
  .subscribe(msg => {
    this.mensajes.push(msg.texto);
  });
```
