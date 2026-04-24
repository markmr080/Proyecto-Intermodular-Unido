import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoomService, Room } from '../services/room.service';

export interface Personaje {
  id: number;
  nombre: string;
  faccion: string;
  imagen: string;
  stats: {
    fuerza: number;
    resistencia: number;
    velocidad: number;
    liderazgo: number;
  };
  descripcion: string;
}

@Component({
  selector: 'app-seleccion-personajes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seleccion-personajes.component.html',
  styleUrl: './seleccion-personajes.component.css'
})
export class SeleccionPersonajesComponent implements OnInit {
  authService = inject(AuthService);
  roomService = inject(RoomService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  personajes: Personaje[] = [
    {
      id: 1,
      nombre: 'Primaris Space Marine',
      faccion: 'Adeptus Astartes',
      imagen: 'https://api.dicebear.com/7.x/bottts/svg?seed=SpaceMarine&backgroundColor=1a1a2e',
      stats: { fuerza: 90, resistencia: 85, velocidad: 70, liderazgo: 80 },
      descripcion: 'Súper soldado del Emperador, forjado para la guerra eterna.'
    },
    {
      id: 2,
      nombre: 'Caos Guerrero',
      faccion: 'Chaos Space Marines',
      imagen: 'https://api.dicebear.com/7.x/bottts/svg?seed=ChaosWarrior&backgroundColor=3d0000',
      stats: { fuerza: 95, resistencia: 80, velocidad: 65, liderazgo: 75 },
      descripcion: 'Renegado corrompido por los dioses del Caos.'
    },
    {
      id: 3,
      nombre: 'Eldar Guardián',
      faccion: 'Craftworld Eldar',
      imagen: 'https://api.dicebear.com/7.x/bottts/svg?seed=EldarGuardian&backgroundColor=001a3d',
      stats: { fuerza: 65, resistencia: 60, velocidad: 95, liderazgo: 70 },
      descripcion: 'Guerrero ágil de una raza ancestral en declive.'
    },
    {
      id: 4,
      nombre: 'Necron Guerrero',
      faccion: 'Necrons',
      imagen: 'https://api.dicebear.com/7.x/bottts/svg?seed=NecronWarrior&backgroundColor=003d1a',
      stats: { fuerza: 80, resistencia: 100, velocidad: 55, liderazgo: 65 },
      descripcion: 'Ser inmortal de metal y energía que despertó del sueño eterno.'
    },
    {
      id: 5,
      nombre: 'Ork Nob',
      faccion: 'Orks',
      imagen: 'https://api.dicebear.com/7.x/bottts/svg?seed=OrkNob&backgroundColor=2d3d00',
      stats: { fuerza: 100, resistencia: 90, velocidad: 60, liderazgo: 55 },
      descripcion: 'Bruto salvaje que vive para la batalla y el WAAAGH!'
    }
  ];

  indiceActual = 0;

  // Datos de la sala
  roomCode = '';
  currentRoom: Room | undefined;

  // Jugadores
  jugador1: { username: string; avatar: string } | null = null;
  jugador2: { username: string; avatar: string } | null = null;

  // Personaje seleccionado por cada jugador (índice)
  seleccionJugador1: number | null = null;
  seleccionJugador2: number | null = null;

  // ¿El usuario actual es el jugador 1 (owner) o el 2?
  jugadorActual: 1 | 2 = 1;

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (!user) {
      this.router.navigate(['/login']);
      return;
    }

    // Leer el código de sala enviado desde partida.ts
    this.roomCode = this.route.snapshot.queryParamMap.get('code') || '';

    // ── GUARDIA: sin código de sala → volver a la lista
    if (!this.roomCode) {
      this.router.navigate(['/lista-salas']);
      return;
    }

    this.currentRoom = this.roomService.getRoomByCode(this.roomCode);

    // ── GUARDIA: la sala no existe o ya expiró → volver a la lista
    if (!this.currentRoom) {
      this.router.navigate(['/lista-salas']);
      return;
    }

    // ── GUARDIA: la sala no tiene 2 jugadores → volver a la sala de espera
    if (this.currentRoom.currentPlayers < 2) {
      this.router.navigate(['/partida', this.roomCode]);
      return;
    }

    // ── A partir de aquí la sala es válida y tiene 2 jugadores ──

    const ownerName = this.currentRoom.ownerName || 'Jugador 1';

    // Jugador 1 = dueño de la sala
    this.jugador1 = {
      username: ownerName,
      avatar: this.currentRoom.ownerPic ||
        `https://api.dicebear.com/7.x/adventurer/svg?seed=${ownerName}`
    };

    // Jugador 2 = el usuario actual si no es el owner, o un nombre genérico
    const j2Name = user.username !== ownerName ? user.username : 'Jugador 2';
    this.jugador2 = {
      username: j2Name,
      avatar: `https://api.dicebear.com/7.x/adventurer/svg?seed=${j2Name}`
    };

    // Determinar si el usuario logueado es el owner (J1) o el J2
    this.jugadorActual = user.username === ownerName ? 1 : 2;
  }

  get personajeActual(): Personaje {
    return this.personajes[this.indiceActual];
  }

  anterior(): void {
    this.indiceActual = (this.indiceActual - 1 + this.personajes.length) % this.personajes.length;
  }

  siguiente(): void {
    this.indiceActual = (this.indiceActual + 1) % this.personajes.length;
  }

  seleccionar(): void {
    if (this.jugadorActual === 1) {
      this.seleccionJugador1 = this.indiceActual;
    } else {
      this.seleccionJugador2 = this.indiceActual;
    }
  }

  get jugador1Listo(): boolean {
    return this.seleccionJugador1 !== null;
  }

  get jugador2Listo(): boolean {
    return this.seleccionJugador2 !== null;
  }

  get ambosProntos(): boolean {
    return this.jugador1Listo && this.jugador2Listo;
  }

  empezarPartida(): void {
    if (!this.ambosProntos) return;
    // Navegar a la partida real
    this.router.navigate(['/partida-activa', this.roomCode]);
  }

  statPorcentaje(valor: number): string {
    return `${valor}%`;
  }
}
