import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import * as io from 'socket.io-client';

@Injectable({
  providedIn: 'root'
})
export class SocketService {
  private socket!: any;

  /** Flag que indica que la desconexión fue iniciada manualmente (ngOnDestroy). */
  private _desconexionIntencionada = false;

  // --- Subjects para Lobby ---
  public nuevaSolicitud$ = new Subject<any>();
  public solicitudAceptada$ = new Subject<string>();
  public solicitudRechazada$ = new Subject<string>();
  public jugadorUnido$ = new Subject<any>();
  public salaCerrada$ = new Subject<string>();
  public partidaIniciada$ = new Subject<string>();
  public personajeSeleccionado$ = new Subject<any>();
  public juegoComenzado$ = new Subject<string>();
  public partidaCancelada$ = new Subject<string>();
  public jugadorExpulsado$ = new Subject<string>();

  // --- Subjects para Juego ---
  public gameState$ = new Subject<any>();
  public jugadorDesconectado$ = new Subject<{ jugadorId: string; nombre: string }>();
  public jugadorReconectado$  = new Subject<string>();

  // --- Subjects para desconexión PROPIA ---
  /** Emite cuando la conexión WebSocket propia se pierde inesperadamente. */
  public miDesconexion$ = new Subject<void>();
  /** Emite cuando la conexión WebSocket propia se reestablece. */
  public miReconexion$ = new Subject<void>();
  /** Emite cuando el tiempo de reconexión de una partida ha expirado en el servidor. */
  public reconexionExpirada$ = new Subject<string>();

  constructor(private ngZone: NgZone) { }

  public connect(token?: string) {
    const socketUrl = window.location.hostname === 'localhost'
      ? 'http://localhost:8082'
      : `https://${window.location.hostname}`;

    // Si ya existe el socket, comprobamos si el token ha cambiado
    if (this.socket) {
      if (token && this.socket.io.opts.query.token !== token) {
        console.log('[SocketService] Actualizando token del socket...');
        this.socket.io.opts.query.token = token;
        this.socket.disconnect().connect();
      }
      return;
    }

    const finalToken = token || sessionStorage.getItem('auth_token') || '';
    console.log('[SocketService] Conectando con token:', finalToken ? '***' : 'VACÍO');

    this.socket = io.connect(socketUrl, {
      transports: ['websocket'],
      autoConnect: true,
      query: { token: finalToken },
      reconnectionAttempts: 5,
      reconnectionDelay: 2000
    });

    // El evento 'connect' se dispara en la conexión inicial Y en cada reconexión automática.
    // Usamos un flag para distinguir el primer connect de los siguientes (reconexiones).
    let primerConexion = true;
    this.socket.on('connect', () => {
      console.log('[SocketService] Conectado al servidor SocketIO (8082). SessionId:', this.socket?.id);
      if (!primerConexion) {
        // Reconexión automática de socket.io: notificar al componente
        this.ngZone.run(() => this.miReconexion$.next());
      }
      primerConexion = false;
    });

    // --- Listeners de Lobby ---
    this.socket.on('nueva-solicitud', (data: any) => this.ngZone.run(() => this.nuevaSolicitud$.next(data)));
    this.socket.on('solicitud-aceptada', (codigo: string) => this.ngZone.run(() => this.solicitadaAceptadaInternal(codigo)));
    this.socket.on('solicitud-rechazada', (msg: string) => this.ngZone.run(() => this.solicitudRechazada$.next(msg)));
    this.socket.on('jugador-unido', (data: any) => this.ngZone.run(() => this.jugadorUnido$.next(data)));
    this.socket.on('sala-cerrada', (msg: string) => this.ngZone.run(() => this.salaCerrada$.next(msg)));
    this.socket.on('partida-iniciada', (codigo: string) => this.ngZone.run(() => this.partidaIniciada$.next(codigo)));
    this.socket.on('personaje-seleccionado', (data: any) => this.ngZone.run(() => this.personajeSeleccionado$.next(data)));
    this.socket.on('juego-comenzado', (codigo: string) => this.ngZone.run(() => this.juegoComenzado$.next(codigo)));
    this.socket.on('partida-cancelada', (userId: string) => this.ngZone.run(() => this.partidaCancelada$.next(userId)));
    this.socket.on('jugador-expulsado', (userId: string) => this.ngZone.run(() => this.jugadorExpulsado$.next(userId)));

    // --- Listeners de Juego ---
    this.socket.on('gameState', (state: any) => {
      console.log('Nuevo estado de juego recibido:', state);
      this.ngZone.run(() => this.gameState$.next(state));
    });

    // Eventos de desconexión/reconexión del rival
    this.socket.on('jugador-desconectado', (payload: string) => {
      const [jugadorId, nombre] = payload.split('|');
      this.ngZone.run(() => this.jugadorDesconectado$.next({ jugadorId, nombre: nombre || jugadorId }));
    });
    this.socket.on('jugador-reconectado', (jugadorId: string) => {
      this.ngZone.run(() => this.jugadorReconectado$.next(jugadorId));
    });

    this.socket.on('reconexion-expirada', (roomCode: string) => {
      console.log('[SocketService] Reconexión expirada para sala:', roomCode);
      this.ngZone.run(() => this.reconexionExpirada$.next(roomCode));
    });

    // --- Desconexión propia (pérdida de red o servidor caído) ---
    this.socket.on('disconnect', (reason: string) => {
      console.log('[SocketService] Desconectado. Razón:', reason, '| Intencional:', this._desconexionIntencionada);
      if (!this._desconexionIntencionada) {
        // Desconexión inesperada → mostrar popup de reconexión al jugador
        this.ngZone.run(() => this.miDesconexion$.next());
      }
      this._desconexionIntencionada = false; // Reset para próximas desconexiones
    });
  }

  // --- Métodos de Lobby ---
  public registrarUsuario(userId: string) {
    this.connect();
    this.socket.emit('registrar-usuario', userId);
  }

  public joinLobby(roomCode: string) {
    this.connect();
    this.socket.emit('join-lobby', roomCode);
  }

  public solicitarUnirse(codigoSala: string, user: any) {
    this.connect();
    this.socket.emit('solicitar-unirse', {
      codigoSala,
      requesterId: user.username,
      requesterName: user.username,
      requesterAvatar: user.profilePicture
    });
  }

  public aceptarSolicitud(codigoSala: string, requester: any) {
    this.socket.emit('aceptar-solicitud', {
      codigoSala,
      requesterId: requester.requesterId,
      requesterName: requester.requesterName,
      requesterAvatar: requester.requesterAvatar
    });
  }

  public rechazarSolicitud(requesterId: string, mensaje: string = 'Tu solicitud ha sido rechazada.') {
    this.socket.emit('rechazar-solicitud', { requesterId, mensaje });
  }

  public cerrarSala(codigoSala: string) {
    this.socket.emit('cerrar-sala', codigoSala);
  }

  public iniciarPartida(codigoSala: string) {
    this.socket.emit('iniciar-partida', { codigoSala });
  }

  public seleccionarPersonaje(codigoSala: string, userId: string, personajeId: number, personajeTipo: string) {
    this.socket.emit('seleccionar-personaje', { codigoSala, userId, personajeId, personajeTipo });
  }

  public comenzarJuego(codigoSala: string) {
    this.socket.emit('comenzar-juego', codigoSala);
  }

  private solicitadaAceptadaInternal(codigo: string) {
    this.solicitudAceptada$.next(codigo);
  }

  // --- Métodos de Juego ---
  public joinRoom(jugadorId: string, jugadorNombre: string, roomCode: string, personajeId: string = 'WULFRIK', avatar: string = '') {
    this.connect();
    this.socket.emit('join-room', { jugadorId, jugadorNombre, roomCode, personajeId, avatar });
  }

  /**
   * Fuerza una reconexión al servidor y re-emite join-room para reincorporarse
   * a la sala de juego activa tras una desconexión inesperada.
   */
  public reconnectToRoom(jugadorId: string, jugadorNombre: string, roomCode: string, personajeId: string, avatar: string) {
    if (this.socket && !this.socket.connected) {
      this.socket.connect();
    } else if (!this.socket) {
      this.connect();
    }
    // Esperar a que el socket se conecte antes de emitir join-room
    setTimeout(() => {
      this.socket.emit('join-room', { jugadorId, jugadorNombre, roomCode, personajeId, avatar });
    }, 500);
  }

  public colocarBarcos(jugadorId: string, roomCode: string, tablero: string[][]) {
    this.socket.emit('colocar-barcos', { jugadorId, roomCode, tablero });
  }

  public atacar(jugadorId: string, roomCode: string, x: number, y: number) {
    this.socket.emit('atacar', { jugadorId, roomCode, x, y });
  }

  public usarHabilidad(jugadorId: string, roomCode: string, habilidadId: string, x: number = -1, y: number = -1) {
    this.socket.emit('usar-habilidad', { jugadorId, roomCode, habilidadId, x, y });
  }

  /** Emite el evento de rendición. El backend declarará ganador al rival. */
  public rendirse(jugadorId: string, roomCode: string) {
    this.socket.emit('rendirse', { jugadorId, roomCode });
  }

  public abandonarSala(userId: string, codigoSala: string) {
    this.socket.emit('abandonar-sala', { userId, codigoSala });
  }

  public expulsarJugador(targetId: string, codigoSala: string) {
    this.socket.emit('expulsar-jugador', { targetId, codigoSala });
  }

  public disconnect() {
    if (this.socket) {
      this._desconexionIntencionada = true;
      this.socket.disconnect();
      // Permitir recreación del socket en el próximo connect()
      this.socket = null;
    }
  }
}
