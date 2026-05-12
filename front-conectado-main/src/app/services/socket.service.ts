import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import * as io from 'socket.io-client';

@Injectable({
  providedIn: 'root'
})
export class SocketService {
  private socket!: any;

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

  constructor(private ngZone: NgZone) { }

  public connect() {
    if (!this.socket || !this.socket.connected) {
      // Conectamos al backend Netty de WebSockets (PUERTO UNIFICADO 8081)
      const socketUrl = window.location.hostname === 'localhost'
        ? 'http://localhost:8081'
        : `https://${window.location.hostname}`;
      this.socket = io.connect(socketUrl, {
        transports: ['websocket'],
        autoConnect: true
      });

      this.socket.on('connect', () => {
        console.log('Conectado al servidor SocketIO (8081)');
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

      this.socket.on('disconnect', () => {
        console.log('Desconectado del servidor SocketIO');
      });
    }
  }

  // --- Métodos de Lobby ---
  public registrarUsuario(userId: string) {
    this.connect();
    this.socket.emit('registrar-usuario', userId);
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

  public rechazarSolicitud(requesterId: string) {
    this.socket.emit('rechazar-solicitud', requesterId);
  }

  public cerrarSala(codigoSala: string) {
    this.socket.emit('cerrar-sala', codigoSala);
  }

  public iniciarPartida(codigoSala: string) {
    this.socket.emit('iniciar-partida', { codigoSala });
  }

  public seleccionarPersonaje(codigoSala: string, userId: string, personajeId: number) {
    this.socket.emit('seleccionar-personaje', { codigoSala, userId, personajeId });
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
      this.socket.disconnect();
    }
  }
}
