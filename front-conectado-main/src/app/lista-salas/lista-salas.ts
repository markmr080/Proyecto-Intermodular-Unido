import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RoomService, Room } from '../services/room.service';
import { AuthService } from '../services/auth.service';
import { SocketService } from '../services/socket.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-lista-salas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lista-salas.html',
  styleUrl: './lista-salas.css',
})
export class ListaSalas implements OnInit, OnDestroy {
  router = inject(Router);
  roomService = inject(RoomService);
  authService = inject(AuthService);
  socketService = inject(SocketService);

  private destroy$ = new Subject<void>();

  searchCode: string = '';
  allRooms: Room[] = [];
  solicitandoUnirse: boolean = false;
  private refreshInterval: any;

  ngOnInit() {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.socketService.registrarUsuario(user.username);
    }

    this.refreshRoomList();
    // Refrescar cada 10 segundos
    this.refreshInterval = setInterval(() => {
      this.refreshRoomList();
    }, 10000);

    // Escuchar respuestas de solicitudes
    this.socketService.solicitudAceptada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(codigo => {
        this.solicitandoUnirse = false;
        this.router.navigate(['/partida', codigo]);
      });

    this.socketService.solicitudRechazada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(msg => {
        this.solicitandoUnirse = false;
        alert(msg);
      });
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
    this.destroy$.next();
    this.destroy$.complete();

  }

  refreshRoomList() {
    this.roomService.getRooms()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (rooms) => {
          this.allRooms = rooms;
        },
        error: (err) => {
          console.error('Error cargando salas:', err);
        }
      });
  }

  get filteredRooms(): Room[] {
    if (!this.searchCode) {
      return this.allRooms;
    }
    return this.allRooms.filter(room => room.codigoSala.toLowerCase().includes(this.searchCode.toLowerCase()));
  }

  crearSala() {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    const newRoom: Room = {
      jugador1: user.username,
      nombreJugador1: user.username,
      avatarJugador1: user.profilePicture || '',
      estado: 'ESPERANDO',
      codigoSala: Math.random().toString(36).substring(2, 8).toUpperCase()
    };

    this.roomService.createRoom(newRoom).subscribe(createdRoom => {
      this.router.navigate(['/partida', createdRoom.codigoSala]);
    });
  }

  recargarSalas() {
    this.refreshRoomList();
  }

  unirse(room: Room) {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    this.solicitandoUnirse = true;
    this.socketService.solicitarUnirse(room.codigoSala, user);
  }

  cancelarSolicitud() {
    this.solicitandoUnirse = false;
    // El servidor no necesita notificación porque el anfitrión simplemente no aceptará
  }

  salir() {
    this.router.navigate(['/menu']);
  }
}


