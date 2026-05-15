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
  tipo: string;
  imagen: string;
  barcos: Barco[];
  abilities: { name: string; desc: string; type: string; icon: string }[];
}

// Flotas únicas por personaje (coinciden con DataInitializer en el backend)
const FLOTA_WULFRIK: Barco[] = [{ tamano: 5 }, { tamano: 4 }, { tamano: 3 }, { tamano: 3 }, { tamano: 2 }];
const FLOTA_AISLINN: Barco[] = [{ tamano: 5 }, { tamano: 4 }, { tamano: 3 }, { tamano: 2 }, { tamano: 2 }];
const FLOTA_LOKHIR: Barco[] = [{ tamano: 5 }, { tamano: 3 }, { tamano: 3 }, { tamano: 2 }, { tamano: 2 }]; // El de 5 será una cruz
const FLOTA_ARANESSA: Barco[] = [{ tamano: 4 }, { tamano: 4 }, { tamano: 3 }, { tamano: 3 }, { tamano: 2 }];
const FLOTA_IKIT: Barco[] = [{ tamano: 4 }, { tamano: 3 }, { tamano: 3 }, { tamano: 2 }, { tamano: 2 }];
const FLOTA_GELT: Barco[] = [{ tamano: 5 }, { tamano: 4 }, { tamano: 3 }, { tamano: 3 }, { tamano: 2 }];

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
      id: 1, nombre: 'Wulfrik', tipo: 'WULFRIK', imagen: '/imagenes/wulfrik.jpg', barcos: FLOTA_WULFRIK,
      abilities: [
        { name: 'Cazador de Naves', desc: 'Si aciertas a un barco enemigo, ganas un disparo extra.', type: 'passive', icon: '/imagenes/wulfrik_habilidades/pasiva.png' },
        { name: 'Desafío del Errante', desc: 'Lanza un disparo; si fallas, se revela la posición aleatoria de un barco enemigo.', type: 'offensive', icon: '/imagenes/wulfrik_habilidades/ofensiva1.png' },
        { name: 'Colmillo de los Mares', desc: 'Impacta un área en línea horizontal de 3 casillas.', type: 'offensive', icon: '/imagenes/wulfrik_habilidades/ofensiva2.png' },
        { name: 'Favor Ruinoso', desc: 'Escuda una casilla propia aleatoria.', type: 'defensive', icon: '/imagenes/wulfrik_habilidades/defensiva.png' }
      ]
    },
    {
      id: 2, nombre: 'Aislinn', tipo: 'AISLINN', imagen: '/imagenes/aislinn.jpg', barcos: FLOTA_AISLINN,
      abilities: [
        { name: 'Señor del Mar Alto Elfo', desc: '20% probabilidad de ignorar escudos y protecciones.', type: 'passive', icon: '/imagenes/aislinn_habilidades/pasiva.png' },
        { name: 'Corte de Lothern', desc: 'Dos disparos independientes en dos casillas separadas.', type: 'offensive', icon: '/imagenes/aislinn_habilidades/ofensiva1.png' },
        { name: 'Ira de Mathlann', desc: 'Golpea en forma de cruz (5 casillas).', type: 'offensive', icon: '/imagenes/aislinn_habilidades/ofensiva2.png' },
        { name: 'Bruma Marina', desc: 'Despliega una niebla en un área 2x2 que protege tus barcos.', type: 'defensive', icon: '/imagenes/aislinn_habilidades/defensiva.png' }
      ]
    },
    {
      id: 3, nombre: 'Lokhir', tipo: 'LOKHIR', imagen: '/imagenes/Lokhir.png', barcos: FLOTA_LOKHIR,
      abilities: [
        { name: 'Saqueador Especialista', desc: 'Al hundir un barco, revela una casilla del siguiente.', type: 'passive', icon: '/imagenes/lokhir_habilidades/pasiva.png' },
        { name: 'Andanada Druchii', desc: 'Dispara a 3 casillas en diagonal.', type: 'offensive', icon: '/imagenes/lokhir_habilidades/ofensiva1.png' },
        { name: 'Furia Corsaria', desc: 'Bengalas en área 3x3. Revela barcos sin causar daño.', type: 'offensive', icon: '/imagenes/lokhir_habilidades/ofensiva2.png' },
        { name: 'Yelmo del Kraken', desc: 'Protege tu barco más grande (Arca Negra). El escudo desaparece por completo al primer impacto.', type: 'defensive', icon: '/imagenes/lokhir_habilidades/defensiva.png' },
        { name: 'Venganza de Karond Kar', desc: 'Habilidad oculta: se activa al perder el Arca Negra. Bombardeo de 5 disparos aleatorios.', type: 'offensive', icon: '/imagenes/lokhir_habilidades/ofensiva3.png' }
      ]
    },
    {
      id: 4, nombre: 'Aranessa', tipo: 'ARANESSA', imagen: '/imagenes/Aranessa (2).png', barcos: FLOTA_ARANESSA,
      abilities: [
        { name: 'Tripulación de los Muertos', desc: '20% de probabilidad de ignorar el daño recibido.', type: 'passive', icon: '/imagenes/aranessa_habilidades/pasiva.png' },
        { name: 'Pólvora Vampírica', desc: 'El fuego se propaga en cruz si impacta un barco.', type: 'offensive', icon: '/imagenes/aranessa_habilidades/ofensiva1.png' },
        { name: 'Disparo de Saloma', desc: 'Elimina TODOS los escudos del rival y dispara en área 2x2.', type: 'offensive', icon: '/imagenes/aranessa_habilidades/ofensiva2.png' },
        { name: 'Hija de Stromfels', desc: 'Escudo total: el próximo disparo enemigo sobre cualquier casilla fallará.', type: 'defensive', icon: '/imagenes/aranessa_habilidades/defensiva.png' }
      ]
    },
    {
      id: 5, nombre: 'Ikit Claw', tipo: 'IKIT', imagen: '/imagenes/ikitclaw.jpg', barcos: FLOTA_IKIT,
      abilities: [
        { name: 'Ingeniero Brujo de Skryre', desc: '20% de probabilidad de que una habilidad ofensiva no consuma enfriamiento.', type: 'passive', icon: '/imagenes/ikitclaw_habilidades/pasiva.png' },
        { name: 'Rayo de Piedra Bruja', desc: 'Lanza un rayo potente que impacta una casilla y revela las adyacentes en cruz.', type: 'offensive', icon: '/imagenes/ikitclaw_habilidades/ofensiva1.png' },
        { name: 'Cohete de Muerte', desc: 'Impacta un área masiva de 3x3 casillas.', type: 'offensive', icon: '/imagenes/ikitclaw_habilidades/ofensiva2.png' },
        { name: 'Escudo de Piedra Bruja', desc: 'Protege un área aleatoria de 2x2 casillas contra el próximo disparo.', type: 'defensive', icon: '/imagenes/ikitclaw_habilidades/defensiva.png' }
      ]
    },
    {
      id: 6, nombre: 'Balthasar Gelt', tipo: 'GELT', imagen: '/imagenes/gelt.png', barcos: FLOTA_GELT,
      abilities: [
        { name: 'Metalurgia Dorada', desc: 'Al impactar un barco, reduce el enfriamiento de una habilidad activa aleatoria en 1 turno.', type: 'passive', icon: '/imagenes/gelt_habilidades/pasiva.png' },
        { name: 'Transmutación de Plomo', desc: 'Convierte una zona 2x2 en oro: revela barcos y causa daño.', type: 'offensive', icon: '/imagenes/gelt_habilidades/ofensiva1.png' },
        { name: 'Lluvia de Metal', desc: 'Invoca una lluvia de proyectiles que impacta en 3 casillas aleatorias del tablero enemigo.', type: 'offensive', icon: '/imagenes/gelt_habilidades/ofensiva2.png' },
        { name: 'Cuerpo de Hierro', desc: 'Protege todas las casillas de tu barco más grande con escudos mágicos.', type: 'defensive', icon: '/imagenes/gelt_habilidades/defensiva.png' }
      ]
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

  // Modo test
  isTestMode = false;
  showCancelModal = false;
  motivoCancelacion = '';

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
          const id = (data.personajeId === -1) ? null : data.personajeId;
          if (this.jugadorActual === 1) {
            this.seleccionJugador2 = id;
          } else {
            this.seleccionJugador1 = id;
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
    this.socketService.partidaCancelada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(userId => {
        console.log('Partida cancelada por:', userId);
        const user = this.authService.getCurrentUser();
        this.motivoCancelacion = userId === user?.username
          ? 'Has abandonado la sala. La partida ha sido cancelada.'
          : 'El otro jugador ha abandonado la sala. La partida ha sido cancelada.';
        this.showCancelModal = true;
        this.limpiarTokensPartida();
      });

    // Escuchar si el anfitrión cierra la sala
    this.socketService.salaCerrada$
      .pipe(takeUntil(this.destroy$))
      .subscribe(msg => {
        console.log('Sala cerrada:', msg);
        this.motivoCancelacion = msg || 'La sala ha sido cerrada.';
        this.showCancelModal = true;
        this.limpiarTokensPartida();
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

    // Clave con username para evitar colisiones si dos jugadores usan el mismo navegador/dispositivo
    const tipoPersonaje = this.personajes[this.indiceActual].tipo;
    localStorage.setItem(`personaje_${this.roomCode}_${user.username}`, tipoPersonaje);

    // Notificar al otro jugador
    this.socketService.seleccionarPersonaje(this.roomCode, user.username, this.indiceActual);
  }

  deseleccionar(): void {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    if (this.jugadorActual === 1) {
      this.seleccionJugador1 = null;
    } else {
      this.seleccionJugador2 = null;
    }

    // Eliminar del localStorage
    localStorage.removeItem(`personaje_${this.roomCode}_${user.username}`);

    // Notificar al otro jugador (enviando null como personajeId)
    this.socketService.seleccionarPersonaje(this.roomCode, user.username, -1);
  }

  esSeleccionadoPorMi(): boolean {
    const seleccion = (this.jugadorActual === 1) ? this.seleccionJugador1 : this.seleccionJugador2;
    return seleccion === this.indiceActual;
  }

  algunoSeleccionadoPorMi(): boolean {
    return (this.jugadorActual === 1) ? this.jugador1Listo : this.jugador2Listo;
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

    // Guardar el personaje elegido por J1 con su username en la clave
    const tipoPersonaje = this.personajes[this.seleccionJugador1!].tipo;
    localStorage.setItem(`personaje_${this.roomCode}_${user.username}`, tipoPersonaje);

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

  abandonar(): void {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    // Notificar al servidor (Lobby Socket)
    this.socketService.abandonarSala(user.username, this.roomCode);

    // Si estamos solos o por alguna razón no recibimos el evento, limpiamos y salimos
    this.limpiarTokensPartida();
    this.router.navigate(['/menu']);
  }

  private limpiarTokensPartida(): void {
    const user = this.authService.getCurrentUser();
    // Eliminar con la clave por username (nueva) y la clave legacy sin username
    if (user) {
      localStorage.removeItem(`personaje_${this.roomCode}_${user.username}`);
    }
    localStorage.removeItem(`personaje_${this.roomCode}`);
    localStorage.removeItem(`test_mode_${this.roomCode}`);
  }

  cerrarModalYSalir(): void {
    this.showCancelModal = false;
    this.router.navigate(['/menu']);
  }

  /** Devuelve un array de longitud `n` para usar con @for en el template */
  range(n: number): number[] {
    return Array.from({ length: n }, (_, i) => i);
  }
}
