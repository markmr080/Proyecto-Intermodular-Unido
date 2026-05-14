# Guía Final: Integración HTTP + WebSockets (Proyecto Real)

Este es el resumen del código que hemos implementado para tu prueba de mañana.

## 1. Backend (Spring Boot - Java)
Ubicación: `com.cifpaviles.proyectofinal.CLMM.api.controller.ExamenController`

```java
@RestController
@RequestMapping("/api/examen")
public class ExamenController {
    
    @Autowired
    private SocketIOServer socketServer;

    // BOTÓN -> HTTP -> SOCKET
    @PostMapping("/notificar")
    public void notificarLobby(@RequestBody Map<String, String> body) {
        String text = body.get("mensaje"); // Ojo: clave en minúscula
        socketServer.getBroadcastOperations().sendEvent("alerta_narrador", Map.of("mensaje", text));
    }

    // SOCKET -> SOCKET (Opcional)
    @Autowired
    public ExamenController(SocketIOServer server) {
        this.socketServer = server;
        this.socketServer.addEventListener("ping_examen", String.class, (client, data, ackSender) -> {
            client.sendEvent("alerta_narrador", Map.of("mensaje", "Respuesta: " + data));
        });
    }
}
```

## 2. Frontend Servicio (Angular)
Ubicación: `src/app/services/examen.service.ts`

```typescript
import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { Socket } from "socket.io-client";
import * as io from 'socket.io-client';

@Injectable({ providedIn: 'root' })
export class ExamenService {
    private socket!: Socket;
    private http = inject(HttpClient);

    constructor() {
        // Fix para el error "io is not defined"
        this.socket = (io as any).connect('http://localhost:8081', { transports: ['websocket'] });
    }

    public listen<T>(event: string): Observable<T> {
        return new Observable((subscriber) => {
            const handler = (data: T) => subscriber.next(data);
            this.socket.on(event, handler);
            return () => this.socket.off(event, handler);
        });
    }

    public dispararAlerta(msg: string) {
        return this.http.post('http://localhost:8080/api/examen/notificar', { mensaje: msg });
    }
}
```

## 3. Frontend Componentes

### Envío (MenuComponent)
```typescript
// En menu.component.ts
private examenService = inject(ExamenService);

mandarAviso() {
  this.examenService.dispararAlerta("Hola lobby").subscribe();
}
```

### Recepción (ListaSalas)
```typescript
// En lista-salas.ts
private eventSub!: Subscription;
private examenService = inject(ExamenService);

ngOnInit() {
  this.eventSub = this.examenService.listen<{ mensaje: string }>('alerta_narrador')
    .subscribe((data) => {
       alert(data.mensaje); // ¡Aquí se ve el resultado!
    });
}

ngOnDestroy() {
  if (this.eventSub) this.eventSub.unsubscribe(); // Limpieza vital
}
```

## 4. Botón HTML (Super-Visible)
Copia esto si el botón se te pierde en el examen:

```html
<div style="position: fixed; top: 20px; left: 20px; z-index: 9999;">
  <button (click)="mandarAviso()" 
          style="background: #ff4d4d; color: white; padding: 10px 20px; border-radius: 5px; font-weight: bold; border: none; cursor: pointer; box-shadow: 0 0 10px rgba(0,0,0,0.5);">
    PROBAR EXAMEN
  </button>
</div>
```

---
**Recuerda**: Para probarlo necesitas dos pestañas. Una en el Menú para pulsar el botón y otra en la Lista de Salas para recibir la notificación.
