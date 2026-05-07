import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { SocketService } from '../services/socket.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-partida-activa',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './partida-activa.component.html',
  styleUrl: './partida-activa.component.css'
})
export class PartidaActivaComponent implements OnInit, OnDestroy {
  route = inject(ActivatedRoute);
  socketService = inject(SocketService);
  authService = inject(AuthService);

  roomCode = '';
  myUsername = '';
  myDisplayName = '';

  gameState: any = null;
  enemyBoard: string[][] = [];

  // --- Lógica de Colocación de Barcos ---
  myBoard: string[][] = [];
  shipsToPlace = [5, 4, 3, 3, 2];
  currentShipIndex = 0;
  orientation: 'H' | 'V' = 'H'; // Horizontal o Vertical
  colocacionTerminada = false;

  constructor() {
    this.myUsername = this.authService.getCurrentUsername();
    // En el futuro puedes sacar el nombre real del perfil, ahora usamos el username
    this.myDisplayName = this.myUsername;

    // Inicializar tableros visuales
    for (let i = 0; i < 10; i++) {
      this.enemyBoard[i] = [];
      this.myBoard[i] = [];
      for (let j = 0; j < 10; j++) {
        this.enemyBoard[i][j] = 'AGUA';
        this.myBoard[i][j] = 'AGUA';
      }
    }
  }

  ngOnInit(): void {
    this.roomCode = this.route.snapshot.paramMap.get('code') || '';

    // Conectar al websocket y entrar a la sala
    this.socketService.connect();
    // Le damos 500ms para asegurar la conexión de socketio subyacente
    setTimeout(() => {
      this.socketService.joinRoom(this.myUsername, this.myDisplayName, this.roomCode);
    }, 500);

    // Escuchar el estado de la partida
    this.socketService.gameState$.subscribe((state) => {
      this.gameState = state;
      this.actualizarTablerosVisuales();
    });
  }

  ngOnDestroy(): void {
    this.socketService.disconnect();
  }

  // --- Lógica Visual y Datos Helper ---

  get miJugador(): any {
    if (!this.gameState) return null;
    return this.gameState.jugador1.id === this.myUsername ? this.gameState.jugador1 : this.gameState.jugador2;
  }

  get enemigo(): any {
    if (!this.gameState) return null;
    return this.gameState.jugador1.id === this.myUsername ? this.gameState.jugador2 : this.gameState.jugador1;
  }

  get esMiTurno(): boolean {
    if (!this.gameState) return false;
    return this.gameState.turnoActualId === this.myUsername;
  }

  actualizarTablerosVisuales() {
    if (this.enemigo && this.enemigo.tablero) {
      this.enemyBoard = this.enemigo.tablero;
    }
    // Si la partida ya empezó, recibimos nuestro tablero con los disparos del enemigo
    if (this.miJugador && this.miJugador.tablero && this.gameState.fase === 'COMBATE') {
      this.myBoard = this.miJugador.tablero;
    }
  }

  // --- Acciones de Fase COLOCACION ---

  cambiarOrientacion() {
    this.orientation = this.orientation === 'H' ? 'V' : 'H';
  }

  colocarBarcoEnCelda(x: number, y: number) {
    if (this.colocacionTerminada || this.gameState?.fase !== 'COLOCACION') return;

    const size = this.shipsToPlace[this.currentShipIndex];

    // Verificar límites
    if (this.orientation === 'H' && y + size > 10) return;
    if (this.orientation === 'V' && x + size > 10) return;

    // Verificar colisiones
    for (let i = 0; i < size; i++) {
      const cx = this.orientation === 'V' ? x + i : x;
      const cy = this.orientation === 'H' ? y + i : y;
      if (this.myBoard[cx][cy] === 'BARCO') return; // Colisión
    }

    // Colocar
    for (let i = 0; i < size; i++) {
      const cx = this.orientation === 'V' ? x + i : x;
      const cy = this.orientation === 'H' ? y + i : y;
      this.myBoard[cx][cy] = 'BARCO';
    }

    this.currentShipIndex++;
    if (this.currentShipIndex >= this.shipsToPlace.length) {
      this.colocacionTerminada = true;
    }
  }

  enviarTablero() {
    if (this.colocacionTerminada) {
      this.socketService.colocarBarcos(this.myUsername, this.roomCode, this.myBoard);
    }
  }

  // --- Acciones de Fase COMBATE ---

  atacarCasilla(x: number, y: number) {
    if (this.gameState?.fase !== 'COMBATE' || !this.esMiTurno || this.gameState.faseReaccion) return;
    this.socketService.atacar(this.myUsername, this.roomCode, x, y);
  }

  usarHabilidad(habilidadId: string) {
    if (this.gameState?.fase !== 'COMBATE' || !this.esMiTurno) return;
    this.socketService.usarHabilidad(this.myUsername, this.roomCode, habilidadId);
  }

  // --- Helpers HTML ---

  getClaseCasilla(estado: string, esMiTablero: boolean): string {
    switch (estado) {
      case 'AGUA': return 'casilla-agua';
      case 'AGUA_GOLPEADA': return 'casilla-fallo';
      case 'TOCADO': return 'casilla-tocado';
      case 'HUNDIDO': return 'casilla-hundido';
      case 'BARCO':
        return esMiTablero ? 'casilla-barco' : 'casilla-agua'; // Enemigo no ve barcos intactos
      default: return 'casilla-agua';
    }
  }

  get habilidadesOfensivas(): any[] {
    if (!this.miJugador || !this.miJugador.personaje) return [];
    return this.miJugador.personaje.habilidadesActivas.filter((h: any) => h.tipo === 'OFENSIVA');
  }

  get habilidadesDefensivas(): any[] {
    if (!this.miJugador || !this.miJugador.personaje) return [];
    return this.miJugador.personaje.habilidadesActivas.filter((h: any) => h.tipo === 'DEFENSIVA');
  }
}
