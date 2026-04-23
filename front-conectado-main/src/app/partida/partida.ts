import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RoomService, Room } from '../services/room.service';
import { AuthService } from '../services/auth.service';
import { SocketService } from '../services/socket.service';
import { Subject, takeUntil } from 'rxjs';

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
  socketService = inject(SocketService);

  private destroy$ = new Subject<void>();

  isOwner = false;
  roomCode = '';
  currentRoom: Room | undefined;

  player1 = { name: 'Jugador 1', role: 'Owner', avatar: '' };
  player2: { name: string, role: string, avatar: string } | null = null;

  pendingPlayers: any[] = [];

  ngOnInit() {
    this.roomCode = this.route.snapshot.paramMap.get('code') || '';
    
    const user = this.authService.getCurrentUser();
    if (user) {
      this.socketService.registrarUsuario(user.username);
    }

    this.roomService.getRoomByCode(this.roomCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe(room => {
        this.currentRoom = room;

        if (this.currentRoom) {
          this.player1.name = this.currentRoom.nombreJugador1;
          this.player1.avatar = this.currentRoom.avatarJugador1;

          if (user && this.currentRoom.jugador1 === user.username) {
            this.isOwner = true;
          }

          if (this.currentRoom.jugador2) {
            this.player2 = { 
              name: this.currentRoom.nombreJugador2 || '', 
              role: 'Jugador', 
              avatar: this.currentRoom.avatarJugador2 || '' 
            };
          }
        } else {
          this.router.navigate(['/lista-salas']);
        }
      });

    // Escuchar nuevas solicitudes (Solo si soy owner)
    this.socketService.nuevaSolicitud$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        if (this.isOwner) {
          if (!this.pendingPlayers.find(p => p.requesterId === data.requesterId)) {
            this.pendingPlayers.push(data);
          }
        }
      });

    // Escuchar cuando un jugador se une oficialmente
    this.socketService.jugadorUnido$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        this.player2 = {
          name: data.requesterName,
          role: 'Jugador',
          avatar: data.requesterAvatar
        };
        this.pendingPlayers = [];
      });

    // Escuchar si la sala se cierra
    this.socketService.salaCerrada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(msg => {
        alert(msg);
        this.router.navigate(['/lista-salas']);
      });
  }

  ngOnDestroy() {
    // Si soy el owner y salgo, cerramos la sala en el servidor/DB
    if (this.isOwner && this.roomCode) {
      this.socketService.cerrarSala(this.roomCode);
    }
    
    this.destroy$.next();
    this.destroy$.complete();
  }

  aceptarJugador(player: any) {
    if (this.player2) return;
    this.socketService.aceptarSolicitud(this.roomCode, player);
  }

  rechazarJugador(player: any) {
    this.pendingPlayers = this.pendingPlayers.filter(p => p.requesterId !== player.requesterId);
    this.socketService.rechazarSolicitud(player.requesterId);
  }

  salir() {
    this.router.navigate(['/lista-salas']);
  }

  empezar() {
    console.log('Enviando a selección de personaje');
  }
}



