import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RoomService, Room } from '../services/room.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-partida',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './partida.html',
  styleUrl: './partida.css',
})
export class Partida implements OnInit, OnDestroy {
  router = inject(Router);
  route = inject(ActivatedRoute);
  roomService = inject(RoomService);
  authService = inject(AuthService);

  isOwner = false;

  timeRemaining = '15:00';
  private totalSeconds = 0;
  private timerInterval: any;

  roomCode = '';
  currentRoom: Room | undefined;

  player1 = { name: 'Jugador 1', role: 'Owner' };
  player2: { name: string, role: string } | null = null;

  pendingPlayers: any[] = [];

  ngOnInit() {
    this.roomCode = this.route.snapshot.paramMap.get('code') || '';
    this.currentRoom = this.roomService.getRoomByCode(this.roomCode);

    if (this.currentRoom) {
      if (this.currentRoom.ownerName) {
        this.player1.name = this.currentRoom.ownerName;
      }
      const user = this.authService.getCurrentUser();
      if (user && this.currentRoom.ownerName === user.username) {
        this.isOwner = true;
      }

      this.totalSeconds = this.roomService.getTimeLeftSeconds(this.currentRoom);
      this.updateTimeString();
      this.startTimer();
    } else {
      // Si la sala no existe o ha expirado, volvemos a la lista
      this.router.navigate(['/lista-salas']);
    }
  }

  ngOnDestroy() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  startTimer() {
    this.timerInterval = setInterval(() => {
      if (this.totalSeconds > 0) {
        this.totalSeconds--;
        this.updateTimeString();
      } else {
        this.cerrarSalaAutomatica();
      }
    }, 1000);
  }

  updateTimeString() {
    const minutes = Math.floor(this.totalSeconds / 60);
    const seconds = this.totalSeconds % 60;
    this.timeRemaining = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  cerrarSalaAutomatica() {
    clearInterval(this.timerInterval);
    console.log('Tiempo agotado. Cerrando sala...');
    this.router.navigate(['/lista-salas']);
  }

  expulsar() {
    console.log('Expulsando jugador');
    this.player2 = null;
  }

  aceptarJugador(player: any) {
    if (this.player2) return;

    console.log('Aceptando jugador', player.name);
    this.player2 = { name: player.name, role: 'Jugador que se une a la sala' };
    this.pendingPlayers = this.pendingPlayers.filter(p => p.id !== player.id);
  }

  rechazarJugador(player: any) {
    console.log('Rechazando jugador', player.name);
    this.pendingPlayers = this.pendingPlayers.filter(p => p.id !== player.id);
  }

  salir() {
    if (this.isOwner) {
      if (this.player2) {
        this.roomService.updateRoomOwner(this.roomCode, this.player2.name);
      } else {
        this.roomService.deleteRoom(this.roomCode);
      }
    }
    this.router.navigate(['/lista-salas']);
  }

  empezar() {
    console.log('Enviando a selección de personajes');
    this.router.navigate(['/seleccion-personajes'], { queryParams: { code: this.roomCode } });
  }

  testPartida() {
    console.log('Modo TEST: Enviando a selección de personajes');
    this.router.navigate(['/seleccion-personajes'], { queryParams: { code: this.roomCode } });
  }
}



