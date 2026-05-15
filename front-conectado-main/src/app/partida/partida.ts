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
  showSettingsModal = false;

  timeRemaining = '15:00';
  private totalSeconds = 15 * 60;
  private timerInterval: any;

  player1 = { name: 'Jugador 1', role: 'Owner', avatar: '' };
  player2: { name: string, role: string, avatar: string } | null = null;

  pendingPlayers: any[] = [];

  ngOnInit() {
    this.roomCode = this.route.snapshot.paramMap.get('code') || '';
    
    const user = this.authService.getCurrentUser();
    if (user) {
      console.log('Partida: Registrando usuario en el socket:', user.username);
      this.socketService.registrarUsuario(user.username);
    }

    if (this.roomCode) {
      this.socketService.joinLobby(this.roomCode);
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

          this.startTimer();
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
    
    // Escuchar si la partida comienza (Sincronización)
    this.socketService.partidaIniciada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(codigo => {
        console.log('Partida: Recibido partida-iniciada para sala:', codigo);
        if (codigo === this.roomCode) {
          console.log('Partida: ¡Navegando a selección de personajes!');
          this.router.navigate(['/seleccion-personajes'], { queryParams: { code: this.roomCode } });
        } else {
          console.warn('Partida: El código recibido no coincide con la sala actual:', codigo, this.roomCode);
        }
      });

    // Escuchar si expulsamos a alguien o alguien se va
    this.socketService.partidaCancelada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(userId => {
        if (this.player2 && this.player2.name === userId) {
          console.log('El jugador 2 ha abandonado la sala');
          this.player2 = null;
        }
      });

    this.socketService.jugadorExpulsado$
      .pipe(takeUntil(this.destroy$))
      .subscribe(userId => {
        if (this.player2 && this.player2.name === userId) {
          console.log('Jugador expulsado con éxito');
          this.player2 = null;
        }
      });
  }

  ngOnDestroy() {
    // Si salimos de la pantalla sin empezar el juego, avisamos al socket
    // (A menos que la navegación sea a la selección de personajes)
    const isNavigatingToGame = this.router.url.includes('seleccion-personajes');
    if (!isNavigatingToGame) {
      this.abandonarSalaNotificar();
    }

    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  startTimer() {
    if (this.timerInterval) clearInterval(this.timerInterval);
    this.timerInterval = setInterval(() => {
      if (this.totalSeconds > 0) {
        this.totalSeconds--;
        this.updateTimeString();
      }
    }, 1000);
  }

  updateTimeString() {
    const minutes = Math.floor(this.totalSeconds / 60);
    const seconds = this.totalSeconds % 60;
    this.timeRemaining = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  expulsar() {
    if (this.player2 && this.isOwner) {
      console.log('Expulsando jugador:', this.player2.name);
      this.socketService.expulsarJugador(this.player2.name, this.roomCode);
    }
  }

  aceptarJugador(player: any) {
    if (this.player2) return;
    this.socketService.aceptarSolicitud(this.roomCode, player);

    // Rechazar automáticamente a los demás solicitantes
    const otrosSolicitantes = this.pendingPlayers.filter(p => p.requesterId !== player.requesterId);
    for (const otro of otrosSolicitantes) {
      this.socketService.rechazarSolicitud(otro.requesterId, 'La sala ya está llena.');
    }
    this.pendingPlayers = [];
  }

  rechazarJugador(player: any) {
    this.pendingPlayers = this.pendingPlayers.filter(p => p.requesterId !== player.requesterId);
    this.socketService.rechazarSolicitud(player.requesterId, 'El anfitrión ha rechazado tu solicitud.');
  }

  abandonarSalaNotificar() {
    const user = this.authService.getCurrentUser();
    if (user && this.roomCode) {
      this.socketService.abandonarSala(user.username, this.roomCode);
    }
  }

  salir() {
    this.abandonarSalaNotificar();
    this.router.navigate(['/lista-salas']);
  }

  empezar() {
    console.log('Solicitando inicio de partida...');
    this.socketService.iniciarPartida(this.roomCode);
  }

  testPartida() {
    this.router.navigate(['/seleccion-personajes'], { queryParams: { code: this.roomCode, testMode: 'true' } });
  }

  abrirAjustes() {
    // Solo mostramos el modal en dispositivos móviles (<= 600px)
    if (window.innerWidth <= 600) {
      this.showSettingsModal = true;
    }
  }

  cerrarAjustes() {
    this.showSettingsModal = false;
  }
}



