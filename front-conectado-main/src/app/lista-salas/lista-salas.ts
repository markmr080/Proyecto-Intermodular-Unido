import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RoomService, Room } from '../services/room.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-lista-salas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './lista-salas.html',
  styleUrl: './lista-salas.css',
})
export class ListaSalas implements OnInit {
  router = inject(Router);
  roomService = inject(RoomService);
  authService = inject(AuthService);

  searchCode: string = '';
  allRooms: Room[] = [];
  private refreshInterval: any;

  ngOnInit() {
    this.refreshRoomList();
    // Refrescar cada 10 segundos para ver si expiran salas
    this.refreshInterval = setInterval(() => {
      this.refreshRoomList();
    }, 10000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  refreshRoomList() {
    this.allRooms = this.roomService.getRooms();
  }

  get filteredRooms(): Room[] {
    if (!this.searchCode) {
      return this.allRooms;
    }
    return this.allRooms.filter(room => room.code.toLowerCase().includes(this.searchCode.toLowerCase()));
  }

  crearSala() {
    const user = this.authService.getCurrentUser();
    const ownerName = user ? user.username : 'Jugador';
    const ownerPic = user ? user.profilePicture || '' : 'https://api.dicebear.com/7.x/adventurer/svg?seed=Guest';

    const newRoom = this.roomService.createRoom(`Partida de ${ownerName}`, ownerPic, ownerName);
    this.router.navigate(['/partida', newRoom.code]);
  }

  recargarSalas() {
    this.refreshRoomList();
    // window.location.reload(); // Opcional, pero refreshRoomList es más SPA-friendly
  }

  unirse(room: Room) {
    if (room.currentPlayers < room.maxPlayers) {
      this.router.navigate(['/partida', room.code]);
    }
  }

  salir() {
    this.router.navigate(['/menu']);
  }
}


