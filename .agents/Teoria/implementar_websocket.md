# Implementar: Señal desde el Frontend → Broadcast a todos en la sala

---

## 📦 PASO 1 — Backend: Registrar el listener en `SocketService.java`

Añade el nuevo listener dentro del método `registrarListeners()`, antes del cierre `}` final de ese método (línea ~315):

```java
// Mensaje de sala (broadcast a todos los participantes)
server.addEventListener("mensaje-sala", Map.class, (cliente, data, ackRequest) -> {
    String codigoSala = (String) data.get("codigoSala");
    String userId     = (String) data.get("userId");
    String texto      = (String) data.get("texto");   // la "señal" que envía el front

    System.out.println("Sala [" + codigoSala + "]: mensaje de " + userId + " → " + texto);

    Optional<LobbyRoom> partidaOpt = lobbyManager.getRoom(codigoSala);
    if (partidaOpt.isEmpty()) return;

    LobbyRoom sala = partidaOpt.get();

    // Construir el payload que recibirán todos
    Map<String, Object> respuesta = new java.util.HashMap<>();
    respuesta.put("userId",      userId);
    respuesta.put("texto",       texto);
    respuesta.put("codigoSala",  codigoSala);
    respuesta.put("timestamp",   System.currentTimeMillis());

    // Enviar a jugador 1
    UUID j1Socket = userSockets.get(sala.getJugador1());
    if (j1Socket != null)
        server.getClient(j1Socket).sendEvent("mensaje-sala-recibido", respuesta);

    // Enviar a jugador 2 (si existe)
    if (sala.getJugador2() != null) {
        UUID j2Socket = userSockets.get(sala.getJugador2());
        if (j2Socket != null)
            server.getClient(j2Socket).sendEvent("mensaje-sala-recibido", respuesta);
    }
});
```

---

## 🅰️ PASO 2 — Frontend: Ampliar `SocketService` (Angular)

### 2a. Añadir el Subject en `socket.service.ts`

En el bloque de Subjects para Lobby (tras la línea 23):

```typescript
// --- Subjects para Lobby ---
public nuevaSolicitud$        = new Subject<any>();
// ... los existentes ...
public mensajeSala$           = new Subject<any>();  // ← NUEVO
```

---

### 2b. Registrar el listener en el método `connect()`

Dentro del bloque de listeners de Lobby (tras la línea ~80):

```typescript
this.socket.on('mensaje-sala-recibido', (data: any) =>
    this.ngZone.run(() => this.mensajeSala$.next(data))
);
```

---

### 2c. Añadir el método emit

Al final de la sección de métodos de Lobby (antes de `joinRoom`):

```typescript
/**
 * Envía una señal/mensaje a todos los participantes de una sala.
 * @param codigoSala  Código de la sala
 * @param userId      ID del emisor
 * @param texto       Contenido del mensaje o señal
 */
public enviarMensajeSala(codigoSala: string, userId: string, texto: string) {
    this.socket.emit('mensaje-sala', { codigoSala, userId, texto });
}
```

---

## 🖥️ PASO 3 — Componente `partida.ts`: emitir y recibir

### 3a. Emitir (botón del front)

```typescript
// En el componente partida.ts
enviarSenal() {
    const user = this.authService.getCurrentUser();
    if (user && this.roomCode) {
        this.socketService.enviarMensajeSala(
            this.roomCode,
            user.username,
            'SENAL_PERSONALIZADA'   // puede ser cualquier string o un objeto serializado
        );
    }
}
```

---

### 3b. Recibir (en `ngOnInit`)

```typescript
// Escuchar mensajes de la sala
this.socketService.mensajeSala$
    .pipe(takeUntil(this.destroy$))
    .subscribe(data => {
        console.log(`Mensaje de sala [${data.codigoSala}] de ${data.userId}: ${data.texto}`);
        // Aquí puedes: actualizar el estado, mostrar una notificación, etc.
        // Ejemplo: mostrar una alerta o actualizar una variable del componente
        this.ultimoMensajeSala = data;
    });
```

---

### 3c. Botón en `partida.html`

```html
<button (click)="enviarSenal()" class="btn-senal">
  Enviar señal
</button>

<!-- Mostrar último mensaje recibido -->
<div *ngIf="ultimoMensajeSala" class="mensaje-sala">
  📢 {{ ultimoMensajeSala.userId }}: {{ ultimoMensajeSala.texto }}
</div>
```
