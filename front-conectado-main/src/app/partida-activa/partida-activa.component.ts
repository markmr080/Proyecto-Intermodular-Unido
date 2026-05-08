import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { SocketService } from '../services/socket.service';
import { AuthService } from '../services/auth.service';
import { RoomService } from '../services/room.service';

@Component({
  selector: 'app-partida-activa',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './partida-activa.component.html',
  styleUrl: './partida-activa.component.css'
})
export class PartidaActivaComponent implements OnInit, OnDestroy {
  route = inject(ActivatedRoute);
  router = inject(Router);
  socketService = inject(SocketService);
  authService = inject(AuthService);
  roomService = inject(RoomService);

  roomCode = '';
  myUsername = '';
  myDisplayName = '';

  gameState: any = null;
  enemyBoard: string[][] = [];

  // --- Lógica de Colocación de Barcos ---
  myBoard: string[][] = [];
  // Lista de tamaños de barcos a colocar; se actualiza desde el gameState del personaje elegido.
  // Valor por defecto (flota estándar) hasta que el backend confirme la flota real.
  shipsToPlace: number[] = [5, 4, 3, 3, 2];
  currentShipIndex = 0;
  orientation: 'H' | 'V' = 'H';
  colocacionTerminada = false;
  flotaInicializada = false; // Evita reinicializar la lista si llega un segundo estado


  // --- Lógica del Modo Test (Bot) ---
  isTestMode = false;
  j2DummyId = 'enemigo-dummy';
  dummyBarcosColocados = false;
  dummyAtacando = false;

  // --- Fin de Partida / Modal ---
  mostrarModal = false;
  soyGanador = false;

  /** Controla la visibilidad del modal "Ver mi tablero" durante el combate. */
  mostrarMiTablero = false;

  /** Controla la visibilidad del diálogo de confirmación de rendición. */
  mostrarConfirmacionRendirse = false;

  // --- Modo targeting para habilidades de área ---
  // Habilidades que requieren seleccionar una celda del tablero enemigo antes de ejecutarse
  private readonly HABILIDADES_CON_TARGET = new Set([
    'SKL_WUL_2', // Colmillo de los Mares — horizontal 3
    'SKL_AIS_2', // Ira de Mathlann — cruz
    'SKL_LOK_1', // Andanada Druchii — diagonal
    'SKL_LOK_2', // Furia Corsaria — revelar 3x3
    'SKL_ARA_1', // Pólvora Vampírica — propagación
  ]);
  /** ID de la habilidad que espera que el jugador clique una celda; null si no hay targeting activo. */
  habilidadPendiente: string | null = null;



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
    this.myUsername = this.authService.getCurrentUsername();
    this.myDisplayName = this.myUsername;

    this.isTestMode = localStorage.getItem(`test_mode_${this.roomCode}`) === 'true';

    console.log('[PartidaActiva] Iniciando con usuario:', this.myUsername, '| Sala:', this.roomCode, '| TestMode:', this.isTestMode);

    // Conectar al websocket y entrar a la sala
    this.socketService.connect();

    // Recuperar el personaje elegido en la pantalla de selección
    const personajeId = localStorage.getItem(`personaje_${this.roomCode}`) || 'WULFRIK';

    // Esperamos a que la conexión esté establecida antes de emitir join-room.
    const intentarJoinRoom = () => {
      console.log('[PartidaActiva] Emitiendo join-room con jugadorId:', this.myUsername, '| personaje:', personajeId);
      this.socketService.joinRoom(this.myUsername, this.myDisplayName, this.roomCode, personajeId);
    };

    setTimeout(() => {
      intentarJoinRoom();
      // Segundo intento de seguridad tras 1.5s adicionales
      setTimeout(intentarJoinRoom, 1500);
    }, 500);

    // Escuchar el estado de la partida
    this.socketService.gameState$.subscribe((state) => {
      console.log('[PartidaActiva] gameState recibido:', {
        turnoActualId: state?.turnoActualId,
        miUsername: this.myUsername,
        esMiTurno: state?.turnoActualId === this.myUsername,
        fase: state?.fase,
        tiempoRestante: state?.tiempoRestante,
        j1id: state?.jugador1?.id,
        j2id: state?.jugador2?.id
      });
      this.gameState = state;
      this.actualizarTablerosVisuales();

      // Inicializar la flota desde el personaje la primera vez que llega el estado
      if (!this.flotaInicializada && state?.fase === 'COLOCACION') {
        const flotaPersonaje = this.miJugador?.personaje?.flotaComoListaTamanos;
        if (flotaPersonaje && flotaPersonaje.length > 0) {
          this.shipsToPlace = flotaPersonaje;
          this.flotaInicializada = true;
          console.log('[PartidaActiva] Flota del personaje:', this.shipsToPlace);
        }
      }

      // Detectar fin de partida para mostrar el modal de victoria/derrota
      if (state?.juegoActivo === false && !this.mostrarModal) {
        this.soyGanador = state?.ganadorId === this.myUsername;
        this.mostrarModal = true;
        // Limpiar el flag de modo test al terminar
        localStorage.removeItem(`test_mode_${this.roomCode}`);
      }

      // Lógica automática del bot si estamos en modo test
      if (this.isTestMode) {
        if (state?.fase === 'COLOCACION' && !this.dummyBarcosColocados) {
          this.colocarBarcosAleatoriosDummy();
        } else if (state?.fase === 'COMBATE' && state?.turnoActualId === this.j2DummyId) {
          if (!this.dummyAtacando) {
            this.dummyAtacando = true;
            setTimeout(() => {
              this.ataqueAleatorioDummy();
            }, 1500); // Retraso para simular tiempo de reacción
          }
        } else if (state?.turnoActualId !== this.j2DummyId) {
          this.dummyAtacando = false; // Resetear flag cuando ya no es su turno
        }
      }
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

    // Verificar colisiones y asegurar un espacio vacío alrededor
    // Recorremos cada parte del barco a colocar
    for (let i = 0; i < size; i++) {
      const cx = this.orientation === 'V' ? x + i : x;
      const cy = this.orientation === 'H' ? y + i : y;

      // Comprobamos la celda actual y todas sus adyacentes (incluyendo diagonales)
      // para asegurar que como mínimo haya un espacio vacío entre cada barco
      for (let dx = -1; dx <= 1; dx++) {
        for (let dy = -1; dy <= 1; dy++) {
          const adjX = cx + dx;
          const adjY = cy + dy;
          // Validar que la celda adyacente esté dentro del tablero (10x10)
          if (adjX >= 0 && adjX < 10 && adjY >= 0 && adjY < 10) {
            if (this.myBoard[adjX][adjY] === 'BARCO') {
              return; // Hay una colisión o un barco adyacente, por lo que no se puede colocar
            }
          }
        }
      }
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

  // --- Lógica del Bot Dummy (Modo Test) ---

  colocarBarcosAleatoriosDummy() {
    this.dummyBarcosColocados = true;
    const dummyBoard: string[][] = [];
    for (let i = 0; i < 10; i++) {
      dummyBoard[i] = [];
      for (let j = 0; j < 10; j++) {
        dummyBoard[i][j] = 'AGUA';
      }
    }

    // Usar la flota real del personaje del bot si está disponible, si no la estándar
    const botShipsToPlace: number[] =
      this.enemigo?.personaje?.flotaComoListaTamanos?.length
        ? this.enemigo.personaje.flotaComoListaTamanos
        : [5, 4, 3, 3, 2];
    
    for (const size of botShipsToPlace) {
      let colocado = false;
      let intentos = 0;
      while (!colocado && intentos < 100) {
        intentos++;
        const orientacionV = Math.random() > 0.5;
        const x = Math.floor(Math.random() * 10);
        const y = Math.floor(Math.random() * 10);

        if (!orientacionV && y + size > 10) continue;
        if (orientacionV && x + size > 10) continue;

        let colision = false;
        // Verificar colisiones y espacios adyacentes para el dummy
        for (let i = 0; i < size; i++) {
          const cx = orientacionV ? x + i : x;
          const cy = !orientacionV ? y + i : y;
          
          for (let dx = -1; dx <= 1; dx++) {
            for (let dy = -1; dy <= 1; dy++) {
              const adjX = cx + dx;
              const adjY = cy + dy;
              if (adjX >= 0 && adjX < 10 && adjY >= 0 && adjY < 10) {
                if (dummyBoard[adjX][adjY] === 'BARCO') colision = true;
              }
            }
          }
        }

        // Si no hay colisión ni barcos pegados, se coloca
        if (!colision) {
          for (let i = 0; i < size; i++) {
            const cx = orientacionV ? x + i : x;
            const cy = !orientacionV ? y + i : y;
            dummyBoard[cx][cy] = 'BARCO';
          }
          colocado = true;
        }
      }
    }

    this.socketService.colocarBarcos(this.j2DummyId, this.roomCode, dummyBoard);
  }

  ataqueAleatorioDummy() {
    // Si la partida ya no está en COMBATE (por ejemplo ha finalizado), no atacamos
    if (this.gameState?.fase !== 'COMBATE') return;

    let attacked = false;
    let intentos = 0;
    while (!attacked && intentos < 100) {
      intentos++;
      const x = Math.floor(Math.random() * 10);
      const y = Math.floor(Math.random() * 10);
      
      // Consultamos el tablero local del jugador real
      const estado = this.myBoard[x][y];
      // Solo ataca si la casilla no fue atacada antes
      if (estado !== 'AGUA_GOLPEADA' && estado !== 'TOCADO' && estado !== 'HUNDIDO') {
        this.socketService.atacar(this.j2DummyId, this.roomCode, x, y);
        attacked = true;
      }
    }
  }

  // --- Acciones de Fase COMBATE ---

  atacarCasilla(x: number, y: number) {
    if (this.gameState?.fase !== 'COMBATE' || !this.esMiTurno) return;

    // Si hay una habilidad de área esperando target, enviamos la habilidad con coordenadas
    if (this.habilidadPendiente) {
      const hab = this.habilidadPendiente;
      this.habilidadPendiente = null;
      this.socketService.usarHabilidad(this.myUsername, this.roomCode, hab, x, y);
      return;
    }

    this.socketService.atacar(this.myUsername, this.roomCode, x, y);
  }

  usarHabilidad(habilidadId: string) {
    if (this.gameState?.fase !== 'COMBATE' || !this.esMiTurno) return;

    // Habilidades con target: activar modo selección de celda
    if (this.HABILIDADES_CON_TARGET.has(habilidadId)) {
      // Si ya estaba activa la misma, la desactiva (toggle)
      this.habilidadPendiente = this.habilidadPendiente === habilidadId ? null : habilidadId;
      return;
    }

    // Habilidades sin target: ejecutar directamente
    this.habilidadPendiente = null;
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

  /**
   * Cuenta cuántos barcos (celdas BARCO intactas) le quedan a un jugador.
   * Se usa para mostrar en el panel de stats durante la fase de combate.
   */
  contarBarcosRestantes(tablero: string[][]): number {
    if (!tablero) return 0;
    let total = 0;
    for (const fila of tablero) {
      for (const celda of fila) {
        if (celda === 'BARCO') total++;
      }
    }
    return total;
  }

  get misBarcosRestantes(): number {
    return this.contarBarcosRestantes(this.miJugador?.tablero);
  }

  get barcosEnemigoRestantes(): number {
    return this.contarBarcosRestantes(this.enemigo?.tablero);
  }

  /**
   * Limpia todos los datos de la partida al salir:
   * 1. Elimina la sala del LobbyManager (evita sala zombie en lista de salas).
   * 2. Limpia claves de localStorage relacionadas con esta sala.
   * 3. Navega a lista de salas con el Router SPA.
   */
  salirDePartida(): void {
    // Eliminar la sala del lobby para que no quede zombie ni bloquee futuras partidas
    this.roomService.deleteRoom(this.roomCode).subscribe({
      error: () => {} // Ignorar error si la sala ya fue eliminada
    });
    localStorage.removeItem(`test_mode_${this.roomCode}`);
    localStorage.removeItem(`personaje_${this.roomCode}`);
    this.router.navigate(['/lista-salas']);
  }

  /**
   * Confirma la rendición:
   * 1. Muestra inmediatamente el modal de derrota para el jugador que se rinde.
   * 2. Emite el evento 'rendirse' al servidor para que el rival vea el modal de victoria.
   */
  confirmarRendirse(): void {
    this.mostrarConfirmacionRendirse = false;
    // Mostrar derrota de forma inmediata en este cliente
    this.soyGanador = false;
    this.mostrarModal = true;
    // Notificar al servidor: el rival recibirá gameState con juegoActivo=false y él como ganador
    this.socketService.rendirse(this.myUsername, this.roomCode);
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
