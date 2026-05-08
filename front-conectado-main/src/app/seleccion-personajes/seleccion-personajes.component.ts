import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoomService, Room } from '../services/room.service';
import { SocketService } from '../services/socket.service';
import { Subject, takeUntil } from 'rxjs';

export interface Barco {
  tamano: number; // 1 a 5 casillas
}

export interface Personaje {
  id: number;
  nombre: string;
  tipo: string; // ID para el backend: WULFRIK, AISLINN, LOKHIR, ARANESSA
  imagen: string;
  barcos: Barco[];
}

// Flotas únicas por personaje (coinciden con DataInitializer en el backend)
const FLOTA_WULFRIK:  Barco[] = [{ tamano: 5 }, { tamano: 4 }, { tamano: 3 }, { tamano: 3 }, { tamano: 2 }];
const FLOTA_AISLINN:  Barco[] = [{ tamano: 5 }, { tamano: 4 }, { tamano: 3 }, { tamano: 2 }, { tamano: 2 }];
const FLOTA_LOKHIR:   Barco[] = [{ tamano: 5 }, { tamano: 3 }, { tamano: 3 }, { tamano: 2 }, { tamano: 2 }];
const FLOTA_ARANESSA: Barco[] = [{ tamano: 4 }, { tamano: 4 }, { tamano: 3 }, { tamano: 3 }, { tamano: 2 }];

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
    { id: 1, nombre: 'Wulfrik',  tipo: 'WULFRIK',  imagen: 'https://i.redd.it/m4wl1apwe6p21.jpg', barcos: FLOTA_WULFRIK },
    { id: 2, nombre: 'Aislinn', tipo: 'AISLINN',  imagen: 'https://static.wikia.nocookie.net/warhammerfb/images/8/8c/AislinnTWWIII1.jpg/revision/latest/scale-to-width-down/1200?cb=20251107155847', barcos: FLOTA_AISLINN },
    { id: 3, nombre: 'Lokhir',  tipo: 'LOKHIR',   imagen: 'https://static.wikia.nocookie.net/labibliotecadelviejomundo/images/9/94/Lokhir_Fellheart_Octava.jpg/revision/latest?cb=20171008101822&path-prefix=es', barcos: FLOTA_LOKHIR },
    { id: 4, nombre: 'Aranessa', tipo: 'ARANESSA', imagen: 'https://cdnb.artstation.com/p/assets/covers/images/030/971/581/large/mauro-matheus-mauro-matheus-aranessathumb.jpg?1602188142', barcos: FLOTA_ARANESSA }
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

  // Modo test
  isTestMode = false;

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

    // Leer modo test de la URL
    this.isTestMode = this.route.snapshot.queryParamMap.get('testMode') === 'true';

    // Escuchar selecciones del otro jugador
    this.socketService.personajeSeleccionado$
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        console.log('Selección remota recibida:', data);
        if (data.codigoSala === this.roomCode) {
          if (this.jugadorActual === 1) {
            this.seleccionJugador2 = data.personajeId;
          } else {
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

    // Guardar el personaje seleccionado para enviarlo en join-room
    const tipoPersonaje = this.personajes[this.indiceActual].tipo;
    localStorage.setItem(`personaje_${this.roomCode}`, tipoPersonaje);

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

  testPartida(): void {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    // Seleccionamos personaje aleatorio para J2
    this.seleccionJugador2 = Math.floor(Math.random() * this.personajes.length);
    
    // Aseguramos que J1 ha seleccionado (el botón ya lo valida, pero reforzamos la lógica)
    if (!this.jugador1Listo) return;

    // Guardar el personaje elegido por J1 para que partida-activa lo use en join-room
    const tipoPersonaje = this.personajes[this.seleccionJugador1!].tipo;
    localStorage.setItem(`personaje_${this.roomCode}`, tipoPersonaje);

    // Activamos el modo test en el storage para que la pantalla de partida lo sepa
    localStorage.setItem(`test_mode_${this.roomCode}`, 'true');

    // Empezamos la partida
    this.empezarPartida();
  }

  empezarPartida(): void {
    if (!this.ambosProntos) return;

    if (this.jugadorActual === 1) {
      this.socketService.comenzarJuego(this.roomCode);
    }
  }

  /** Devuelve un array de longitud `n` para usar con @for en el template */
  range(n: number): number[] {
    return Array.from({ length: n }, (_, i) => i);
  }
}
