Backend (Spring Boot)
Netty-SocketIO (Java)
Paso 1B: Añadir Dependencia Netty-SocketIO
Añadimos la librería Netty-SocketIO al pom.xml. Esta librería levanta un servidor paralelo capaz de entender el protocolo de Socket.IO de forma nativa en Java.

pom.xml
<dependency>
    <groupId>com.corundumstudio.socketio</groupId>
    <artifactId>netty-socketio</artifactId>
    <version>2.0.6</version>
</dependency>
Paso 2: Configurar SocketIOServer
Creamos el Bean de configuración asignándole un puerto propio (ej. 8085) y usamos un CommandLineRunner para arrancar el servidor asíncrono explícitamente.

SocketIOConfig.java
@Configuration
public class SocketIOConfig {

    // 0.0.0.0 para que funcione en contenedores y producción
    @Value("${socketio.host:0.0.0.0}")
    private String host;

    @Value("${socketio.port:8085}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        // ⚠️ CORS abierto (*): Solo para desarrollo. En PROD usa "https://tudominio.com"
        config.setOrigin("*");
        return new SocketIOServer(config);
    }
}

// ⚠️ ¡IMPORTANTE! Netty no arranca solo. Necesitamos iniciarlo:
@Component
public class SocketServerRunner implements CommandLineRunner {
    private final SocketIOServer server;

    public SocketServerRunner(SocketIOServer server) { this.server = server; }

    @Override
    public void run(String... args) { server.start(); }

    @PreDestroy
    public void stop() { server.stop(); }
}
Paso 3: Autorización para JWT
Añadimos un AuthorizationListener a la configuración. Este interceptará la petición de Handshake, extraerá el token de la URL y validará la sesión antes de conectar.

SocketIOConfig.java (AuthorizationListener)
@Configuration
public class SocketIOConfig {

    @Autowired
    private JwtUtils jwtUtils; // Inyectamos nuestra utilidad JWT

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        // ... config (host, port)
        
        config.setAuthorizationListener(data -> {
            // Obtenemos el token inyectado en el handshake
            String token = data.getSingleUrlParam("token");
            
            if (token != null && jwtUtils.validateToken(token)) {
                // Usuario validado con éxito
                return AuthorizationResult.SUCCESSFUL_AUTHORIZATION;
            }
            
            // Conexión rechazada
            return AuthorizationResult.FAILED_AUTHORIZATION;
        });
        
        return new SocketIOServer(config);
    }
}
Paso 5: Listeners en Java (onData)
Registramos un addEventListener en el controlador. Jackson convierte el JSON automáticamente a la clase Java, y usamos BroadcastOperations para enviar la respuesta a todos.

JuegoSocketController.java
@Component
public class JuegoSocketController {

    private final SocketIOServer server;

    @Autowired
    public JuegoSocketController(SocketIOServer server) {
        this.server = server;

        // Escuchamos el evento custom del cliente 'disparo_laser'
        this.server.addEventListener("disparo_laser", DisparoData.class, (client, data, ackSender) -> {
            
            // Validamos lógica de juego y reenviamos el efecto AL RESTO
            AlertaResponse alerta = new AlertaResponse("¡Un jugador ha disparado!");
            
            // server.getBroadcastOperations().sendEvent() emite a todos
            server.getBroadcastOperations().sendEvent("alerta_narrador", alerta);
        });
    }
}



Frontend (Angular)
El cliente Socket.IO
Paso 1A: Instalar Librería Socket.IO
Instalamos el cliente oficial en Angular. Esto nos permite usar una API muy sencilla y familiar para conectarnos al servidor bidireccional.

planetas_front/src/app/core/services/websocket.service.ts
import { WebsocketService } from './core/services/websocket.service';
Paso 3 y 4: Conexión Socket.IO con JWT
Iniciamos la conexión pasándole el token JWT por la query string. Esta es la forma más compatible de autenticarse en el Handshake inicial de Socket.IO con Netty.

planetas_front/src/app/core/services/websocket.service.ts
import { Injectable } from '@angular/core';
import { io, Socket } from 'socket.io-client';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private socket!: Socket;

  // 1. Conexión segura (Usa WSS en producción)
  public conectar(token: string): void {
    this.socket = io('http://localhost:8085', {
      // ⚠️ Query params en URL se loguean. En Socket.IO v3+ prefiere usar:
      // auth: { token } (si netty-socketio/backend lo soporta)
      query: { token },
      transports: ['websocket']
    });
  }

  // 2. Patrón Correcto: Envolver escucha con Genéricos y Desuscripción segura
  public listen<T>(event: string): Observable<T> {
    return new Observable((subscriber) => {
      const handler = (data: T) => subscriber.next(data);
      this.socket.on(event, handler);
      
      // ✅ IMPORTANTE: Pasamos el handler para no borrar otros listeners del mismo evento
      return () => this.socket.off(event, handler);
    });
  }

  // 3. Método centralizado para emitir
  public emit(event: string, data: any): void {
    this.socket.emit(event, data);
  }
}
Paso 5: Suscribirse (on) y Enviar (emit)
Usamos emit para enviar objetos de datos y on (envuelto en un Observable) para reaccionar asíncronamente a los mensajes que vengan del servidor.

planetas_front/src/app/components/panel-de-juego/panel-de-juego.ts
import { inject } from '@angular/core';

export class JuegoComponent {
  private wsService = inject(WebsocketService); // Estilo moderno Angular

  lanzarAtaque() {
    // Emitimos el evento hacia el servidor Java
    this.wsService.emit('disparo_laser', { daño: 100, arma: "RayoX" });
  }
}
Recibirás las respuestas del tópico (topic):

planetas_front/src/app/components/panel-de-juego/panel-de-juego.ts (Suscripción)
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Subscription } from 'rxjs';

export class PanelJuegoComponent implements OnInit, OnDestroy {
  private eventSub!: Subscription;
  private wsService = inject(WebsocketService); // Estilo moderno Angular
  
  ngOnInit() {
    // Escuchamos tipando la respuesta
    this.eventSub = this.wsService.listen<{ mensaje: string }>('alerta_narrador')
      .subscribe((data) => {
         // En app real usar Toast/Snackbar, no alert
         console.log("Notificación del Servidor: " + data.mensaje);
      });
  }

  ngOnDestroy() {
    // Esto dispara la función de limpieza (off) en el Observable
    if (this.eventSub) this.eventSub.unsubscribe();
  }
}