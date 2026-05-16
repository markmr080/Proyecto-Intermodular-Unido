# Guía Visual: ¿Cómo se comunican las piezas de nuestro juego?

Para entender cómo funciona el juego, imagina que tenemos tres personajes trabajando:
1. **Frontend (Angular):** La interfaz que toca el usuario.
2. **Middleware (Spring Boot Sockets):** El "cerebro" en tiempo real que gestiona la partida.
3. **API (Spring Boot Monolito):** El "notario" que guarda los datos en MySQL/MongoDB.

---

## 1. Comunicación por HTTP (Estilo "Carta" ✉️)

Se usa para acciones puntuales: pedir datos o guardar algo permanentemente.

### Ejemplo en el Frontend (Angular)
Cuando pides la lista de salas disponibles, haces una petición HTTP GET.
```typescript
// Archivo: room.service.ts
getRooms(): Observable<Room[]> {
  // HttpClient hace una petición "de ida y vuelta"
  return this.http.get<Room[]>(`${this.apiUrl}/salas`);
}
```

### Ejemplo en el Middleware (Java)
Cuando el árbitro decide borrar una partida de la base de datos oficial.
```java
// Archivo: BackendClient.java
public void eliminarPartida(Long id) {
    // RestClient envía una orden HTTP DELETE a la API
    restClient.delete()
        .uri("/api/partidas/{id}", id)
        .retrieve()
        .toBodilessEntity();
}
```

---

## 2. Comunicación por WebSockets (Estilo "Teléfono" 📞)

Se usa para el tiempo real: eventos que deben llegar al instante sin que nadie los pida.

### Paso 1: El Frontend envía un evento
Cuando el anfitrión acepta a alguien, "dispara" un mensaje por el cable del socket.
```typescript
// Archivo: socket.service.ts
public aceptarSolicitud(codigoSala: string, requester: any) {
  this.socket.emit('aceptar-solicitud', {
    codigoSala,
    requesterId: requester.requesterId
  });
}
```

### Paso 2: El Middleware lo recibe y reacciona
El servidor está escuchando ese cable y, cuando oye el mensaje, ejecuta su lógica y avisa a los demás.
```java
// Archivo: SocketService.java
server.addEventListener("aceptar-solicitud", Map.class, (cliente, data, ackRequest) -> {
    String requesterId = (String) data.get("requesterId");
    
    // El árbitro avisa al otro jugador: "¡Te han aceptado!"
    UUID rSocketId = userSockets.get(requesterId);
    server.getClient(rSocketId).sendEvent("solicitud-aceptada", codigoSala);
});
```

### Paso 3: El Frontend (Jugador 2) recibe la noticia
El código del otro jugador reacciona al evento que acaba de llegar del servidor.
```typescript
// Archivo: lista-salas.ts
this.socketService.solicitudAceptada$.subscribe(codigo => {
    // Al recibir el evento del socket, el navegador cambia de pantalla
    this.router.navigate(['/partida', codigo]);
});
```

---

## 3. Resumen de Flujo

| Acción | Tipo | Origen | Destino |
| :--- | :--- | :--- | :--- |
| **Login / Registro** | **HTTP** | Frontend | Middleware (Puerto 8080) |
| **Crear Sala (DB)** | **HTTP** | Frontend | Middleware (Puerto 8080) |
| **Aceptar Jugador** | **Socket** | Frontend | Middleware (Puerto 8082) |
| **Disparar / Habilidad** | **Socket** | Frontend | Middleware (Puerto 8082) |
| **Finalizar Partida** | **HTTP** | Middleware | API (Puerto 8081) |

> \* *Nota: En una arquitectura ideal de seguridad, incluso la creación de salas debería pasar por el Middleware primero.*

---

## 4. 🛡️ El Middleware como Escudo (Zero Trust)

Como bien has apuntado, en este proyecto se busca que el Middleware sea el **único punto de entrada** para el Frontend.

### El Flujo "Zero Trust" (Confianza Cero):
1. **Frontend -> Middleware (HTTP):** El usuario pide sus estadísticas o se registra llamando al Middleware (Puerto 8080).
2. **Middleware (Validación):** El Middleware comprueba que el usuario es quien dice ser (comprueba tokens y la "huella digital").
3. **Middleware -> API (HTTP Interno):** Si todo está OK, el Middleware le pide los datos a la API (Puerto 8081) usando su propio canal seguro.
4. **Respuesta:** La API le da los datos al Middleware, y el Middleware se los devuelve al Frontend.

### ¿Por qué hacerlo así?
- **Seguridad:** La API puede estar "escondida" y solo responder al Middleware. Nadie desde fuera puede atacarla directamente.
- **Abstracción:** Si mañana cambiamos la base de datos o la API, el Frontend no tiene por qué enterarse, porque él solo habla con el Middleware.

### Estado actual:
Actualmente, el proyecto ya ha centralizado gran parte de la lógica. El Frontend se comunica con el **Puerto 8080** para todas las peticiones HTTP (REST) y con el **Puerto 8082** para los eventos en tiempo real (Sockets). La API (Puerto 8081) permanece oculta, aceptando solo peticiones del Middleware mediante una clave secreta.
