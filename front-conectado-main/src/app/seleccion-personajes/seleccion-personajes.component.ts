import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoomService, Room } from '../services/room.service';
import { SocketService } from '../services/socket.service';
import { Subject, takeUntil } from 'rxjs';

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
export class SeleccionPersonajesComponent implements OnInit, OnDestroy {
  authService = inject(AuthService);
  roomService = inject(RoomService);
  socketService = inject(SocketService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  private destroy$ = new Subject<void>();

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

    this.roomCode = this.route.snapshot.queryParamMap.get('code') || '';
    if (!this.roomCode) {
      this.router.navigate(['/lista-salas']);
      return;
    }

    // Registrar usuario para asegurar socket activo
    this.socketService.registrarUsuario(user.username);

    this.roomService.getRoomByCode(this.roomCode)
      .pipe(takeUntil(this.destroy$))
      .subscribe(room => {
        this.currentRoom = room;

        if (!this.currentRoom) {
          this.router.navigate(['/lista-salas']);
          return;
        }

        const ownerName = this.currentRoom.nombreJugador1 || 'Jugador 1';
        this.jugador1 = {
          username: ownerName,
          avatar: this.currentRoom.avatarJugador1 || `https://api.dicebear.com/7.x/adventurer/svg?seed=${ownerName}`
        };

        const j2Name = (this.currentRoom.nombreJugador2 && this.currentRoom.nombreJugador2 !== ownerName) 
                       ? this.currentRoom.nombreJugador2 
                       : (user.username !== ownerName ? user.username : 'Jugador 2');
        
        this.jugador2 = {
          username: j2Name,
          avatar: this.currentRoom.avatarJugador2 || `https://api.dicebear.com/7.x/adventurer/svg?seed=${j2Name}`
        };

        this.jugadorActual = user.username === ownerName ? 1 : 2;
      });

    // Escuchar selecciones del otro jugador
    this.socketService.personajeSeleccionado$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        console.log('Selección remota recibida:', data);
        if (data.codigoSala === this.roomCode) {
          if (this.jugadorActual === 1) {
            // Soy J1, recibo de J2
            this.seleccionJugador2 = data.personajeId;
          } else {
            // Soy J2, recibo de J1
            this.seleccionJugador1 = data.personajeId;
          }
        }
      });

    // Escuchar inicio de juego real
    this.socketService.juegoComenzado$
      .pipe(takeUntil(this.destroy$))
      .subscribe(codigo => {
        if (codigo === this.roomCode) {
          console.log('¡Juego comenzado! Navegando a partida-activa');
          this.router.navigate(['/partida-activa', this.roomCode]);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
    const user = this.authService.getCurrentUser();
    if (!user) return;

    if (this.jugadorActual === 1) {
      this.seleccionJugador1 = this.indiceActual;
    } else {
      this.seleccionJugador2 = this.indiceActual;
    }

    // Notificar al otro jugador
    this.socketService.seleccionarPersonaje(this.roomCode, user.username, this.indiceActual);
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
    
    // Solo el admin (J1) dispara el evento de inicio real para evitar duplicados, 
    // aunque ambos podrían.
    if (this.jugadorActual === 1) {
      this.socketService.comenzarJuego(this.roomCode);
    }
  }

  statPorcentaje(valor: number): string {
    return `${valor}%`;
  }
}
