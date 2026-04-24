import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import * as io from 'socket.io-client';

@Injectable({
  providedIn: 'root'
})
export class SocketService {
  private socket!: any;
  
  // Observable para emitir el estado de la partida cuando llegue del backend
  public gameState$ = new Subject<any>();

  constructor() { }

  public connect() {
    if (!this.socket) {
      // Conectamos al backend Netty de WebSockets (puerto 8081)
      this.socket = io.connect('http://localhost:8081', {
        transports: ['websocket'],
        autoConnect: true
      });

      this.socket.on('connect', () => {
        console.log('Conectado al servidor de juego SocketIO');
      });

      this.socket.on('gameState', (state: any) => {
        console.log('Nuevo estado recibido:', state);
        this.gameState$.next(state);
      });

      this.socket.on('disconnect', () => {
        console.log('Desconectado del servidor de juego');
      });
    }
  }

  public joinRoom(jugadorId: string, jugadorNombre: string, roomCode: string) {
    if (this.socket) {
      this.socket.emit('join-room', { jugadorId, jugadorNombre, roomCode });
    }
  }

  public colocarBarcos(jugadorId: string, roomCode: string, tablero: string[][]) {
    if (this.socket) {
      this.socket.emit('colocar-barcos', { jugadorId, roomCode, tablero });
    }
  }

  public atacar(jugadorId: string, roomCode: string, x: number, y: number) {
    if (this.socket) {
      this.socket.emit('atacar', { jugadorId, roomCode, x, y });
    }
  }

  public usarHabilidad(jugadorId: string, roomCode: string, habilidadId: string) {
    if (this.socket) {
      this.socket.emit('usar-habilidad', { jugadorId, roomCode, habilidadId });
    }
  }

  public disconnect() {
    if (this.socket) {
      this.socket.disconnect();
    }
  }
}
