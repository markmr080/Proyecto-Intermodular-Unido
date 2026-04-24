import { Injectable } from '@angular/core';
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

  // --- Subjects para Juego ---
  public gameState$ = new Subject<any>();

  constructor() { }

  public connect() {
    if (!this.socket || !this.socket.connected) {
      // Conectamos al backend Netty de WebSockets (PUERTO UNIFICADO 8081)
      this.socket = io.connect('http://localhost:8081', {
        transports: ['websocket'],
        autoConnect: true
      });

      this.socket.on('connect', () => {
        console.log('Conectado al servidor SocketIO (8081)');
      });

      // --- Listeners de Lobby ---
      this.socket.on('nueva-solicitud', (data: any) => this.nuevaSolicitud$.next(data));
      this.socket.on('solicitud-aceptada', (codigo: string) => this.solicitudAceptada$.next(codigo));
      this.socket.on('solicitud-rechazada', (msg: string) => this.solicitudRechazada$.next(msg));
      this.socket.on('jugador-unido', (data: any) => this.jugadorUnido$.next(data));
      this.socket.on('sala-cerrada', (msg: string) => this.salaCerrada$.next(msg));

      // --- Listeners de Juego ---
      this.socket.on('gameState', (state: any) => {
        console.log('Nuevo estado de juego recibido:', state);
        this.gameState$.next(state);
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

  // --- Métodos de Juego ---
  public joinRoom(jugadorId: string, jugadorNombre: string, roomCode: string) {
    this.connect();
    this.socket.emit('join-room', { jugadorId, jugadorNombre, roomCode });
  }

  public colocarBarcos(jugadorId: string, roomCode: string, tablero: string[][]) {
    this.socket.emit('colocar-barcos', { jugadorId, roomCode, tablero });
  }

  public atacar(jugadorId: string, roomCode: string, x: number, y: number) {
    this.socket.emit('atacar', { jugadorId, roomCode, x, y });
  }

  public usarHabilidad(jugadorId: string, roomCode: string, habilidadId: string) {
    this.socket.emit('usar-habilidad', { jugadorId, roomCode, habilidadId });
  }

  public disconnect() {
    if (this.socket) {
      this.socket.disconnect();
    }
  }
}
