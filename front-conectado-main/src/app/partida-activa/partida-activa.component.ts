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

  // --- Propiedades para Drag and Drop de Barcos ---
  /** Barcos que aún no han sido colocados en el tablero. */
  shipsAvailable: { size: number, placed: boolean, id: number }[] = [];
  /** Tamaño del barco que se está arrastrando actualmente. */
  draggedShipIndex: number | null = null;
  /** Mensaje de error cuando una colocación no es válida. */
  placementError: string | null = null;
  private errorTimeout: any;

  // Propiedades para la previsualización del drag
  hoverX: number | null = null;
  hoverY: number | null = null;



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
      const user = this.authService.getCurrentUser();
      const avatarUrl = user?.profilePicture || `https://api.dicebear.com/7.x/adventurer/svg?seed=${this.myUsername}`;
      
      console.log('[PartidaActiva] Emitiendo join-room con jugadorId:', this.myUsername, '| personaje:', personajeId, '| avatar:', avatarUrl);
      this.socketService.joinRoom(this.myUsername, this.myDisplayName, this.roomCode, personajeId, avatarUrl);
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
          this.shipsAvailable = flotaPersonaje.map((size: number, idx: number) => ({
            size,
            placed: false,
            id: idx
          }));
          this.flotaInicializada = true;
          console.log('[PartidaActiva] Flota del personaje inicializada:', this.shipsAvailable);
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

  // --- Drag and Drop Handlers ---

  onDragStart(index: number) {
    this.draggedShipIndex = index;
    // Opcional: podemos resetear el error al empezar a arrastrar
    this.placementError = null;
  }

  onDragOver(event: DragEvent, x: number, y: number) {
    event.preventDefault(); // Permitir el drop
    this.hoverX = x;
    this.hoverY = y;
  }

  onDragLeave() {
    this.hoverX = null;
    this.hoverY = null;
  }

  onMouseEnter(x: number, y: number) {
    if (this.gameState?.fase === 'COLOCACION') {
      this.hoverX = x;
      this.hoverY = y;
    }
  }

  onMouseLeave() {
    this.hoverX = null;
    this.hoverY = null;
  }

  onDrop(event: DragEvent, x: number, y: number) {
    event.preventDefault();
    this.hoverX = null;
    this.hoverY = null;
    
    if (this.draggedShipIndex === null) return;

    const ship = this.shipsAvailable[this.draggedShipIndex];
    if (ship.placed) return;

    if (this.canPlaceShip(x, y, ship.size)) {
      this.placeShipInBoard(x, y, ship.size);
      ship.placed = true;
      this.draggedShipIndex = null;
      this.placementError = null;
      
      // Comprobar si todos los barcos están colocados
      this.colocacionTerminada = this.shipsAvailable.every(s => s.placed);
    } else {
      this.showPlacementError('⚠️ Posición no válida o barcos demasiado juntos');
    }
  }

  /** Determina si una celda debe mostrarse resaltada durante el arrastre o hover */
  isCellHighlighted(x: number, y: number): boolean {
    if (this.hoverX === null || this.hoverY === null || this.gameState?.fase !== 'COLOCACION') return false;

    // Si estamos arrastrando, usamos ese barco. Si no, el siguiente disponible para clic.
    let size = 0;
    if (this.draggedShipIndex !== null) {
      size = this.shipsAvailable[this.draggedShipIndex].size;
    } else {
      const nextShip = this.shipsAvailable.find(s => !s.placed);
      if (!nextShip) return false;
      size = nextShip.size;
    }

    // Calculamos si (x,y) está dentro del rango del barco
    if (this.orientation === 'H') {
      return x === this.hoverX && y >= this.hoverY && y < this.hoverY + size;
    } else {
      return y === this.hoverY && x >= this.hoverX && x < this.hoverX + size;
    }
  }

  /** Indica si la previsualización actual es válida */
  isPreviewValid(): boolean {
    if (this.hoverX === null || this.hoverY === null) return true;

    let size = 0;
    if (this.draggedShipIndex !== null) {
      size = this.shipsAvailable[this.draggedShipIndex].size;
    } else {
      const nextShip = this.shipsAvailable.find(s => !s.placed);
      if (!nextShip) return true;
      size = nextShip.size;
    }

    return this.canPlaceShip(this.hoverX, this.hoverY, size);
  }

  showPlacementError(msg: string) {
    this.placementError = msg;
    if (this.errorTimeout) clearTimeout(this.errorTimeout);
    this.errorTimeout = setTimeout(() => this.placementError = null, 3000);
  }

  /** Comprueba si un barco de cierto tamaño se puede colocar en (x,y) con la orientación actual. */
  canPlaceShip(x: number, y: number, size: number): boolean {
    if (this.orientation === 'H' && y + size > 10) return false;
    if (this.orientation === 'V' && x + size > 10) return false;

    for (let i = 0; i < size; i++) {
      const cx = this.orientation === 'V' ? x + i : x;
      const cy = this.orientation === 'H' ? y + i : y;

      for (let dx = -1; dx <= 1; dx++) {
        for (let dy = -1; dy <= 1; dy++) {
          const adjX = cx + dx;
          const adjY = cy + dy;
          if (adjX >= 0 && adjX < 10 && adjY >= 0 && adjY < 10) {
            if (this.myBoard[adjX][adjY] === 'BARCO') return false;
          }
        }
      }
    }
    return true;
  }

  placeShipInBoard(x: number, y: number, size: number) {
    for (let i = 0; i < size; i++) {
      const cx = this.orientation === 'V' ? x + i : x;
      const cy = this.orientation === 'H' ? y + i : y;
      this.myBoard[cx][cy] = 'BARCO';
    }
  }

  colocarBarcoEnCelda(x: number, y: number) {
    if (this.gameState?.fase !== 'COLOCACION') return;

    // Si ya hay un barco, lo retiramos
    if (this.myBoard[x][y] === 'BARCO') {
      this.retirarBarco(x, y);
      return;
    }

    // Si no hay barco, permitimos colocar el primero disponible
    const nextShip = this.shipsAvailable.find(s => !s.placed);
    if (!nextShip) return;

    if (this.canPlaceShip(x, y, nextShip.size)) {
      this.placeShipInBoard(x, y, nextShip.size);
      nextShip.placed = true;
      this.colocacionTerminada = this.shipsAvailable.every(s => s.placed);
      this.placementError = null;
    } else {
      this.showPlacementError('⚠️ Posición no válida o barcos demasiado juntos');
    }
  }

  /** Retira el barco que ocupa la celda (x, y) */
  retirarBarco(x: number, y: number) {
    // Encontrar todas las celdas contiguas que son BARCO
    const celdasBarco: {x: number, y: number}[] = [];
    const stack = [{x, y}];
    const visited = new Set<string>();

    while (stack.length > 0) {
      const {x: cx, y: cy} = stack.pop()!;
      const key = `${cx},${cy}`;
      if (visited.has(key)) continue;
      visited.add(key);

      if (cx >= 0 && cx < 10 && cy >= 0 && cy < 10 && this.myBoard[cx][cy] === 'BARCO') {
        celdasBarco.push({x: cx, y: cy});
        // Mirar vecinos (solo horizontal y vertical)
        stack.push({x: cx + 1, y: cy});
        stack.push({x: cx - 1, y: cy});
        stack.push({x: cx, y: cy + 1});
        stack.push({x: cx, y: cy - 1});
      }
    }

    const size = celdasBarco.length;
    // Limpiar celdas en el tablero
    celdasBarco.forEach(c => this.myBoard[c.x][c.y] = 'AGUA');

    // Marcar como no colocado en la lista
    // Buscamos un barco del mismo tamaño que esté marcado como colocado
    const shipToRestore = this.shipsAvailable.find(s => s.size === size && s.placed);
    if (shipToRestore) {
      shipToRestore.placed = false;
    }
    
    this.colocacionTerminada = false;
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
      case 'REVELADA':
        // En el tablero ENEMIGO se muestra como celda revelada (sabemos que hay barco).
        // En nuestro tablero propio no aplica este estado, pero por seguridad lo tratamos igual que BARCO.
        return esMiTablero ? 'casilla-barco' : 'casilla-revelada';
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
        // Contamos tanto BARCO como REVELADA: ambos son barcos aun a flote
        if (celda === 'BARCO' || celda === 'REVELADA') total++;
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

  // --- Funciones TrackBy para evitar parpadeos en re-renders ---
  trackByHabilidad(index: number, hab: any): string {
    return hab.id;
  }

  trackByFila(index: number, fila: string[]): number {
    return index;
  }

  trackByCelda(index: number, celda: string): number {
    return index;
  }

  /**
   * Formatea el mensaje de estado reemplazando nombres de personajes por nombres de jugadores.
   */
  get mensajeEstadoFormateado(): string {
    if (!this.gameState || !this.gameState.mensajeEstado) return '';
    
    let msg = this.gameState.mensajeEstado;
    const j1 = this.gameState.jugador1;
    const j2 = this.gameState.jugador2;

    if (j1 && j1.personaje) {
      const nombrePersonaje = j1.personaje.nombre;
      const nombreJugador = j1.nombre;
      // Reemplazo insensible a mayúsculas/minúsculas
      const regex = new RegExp(nombrePersonaje, 'gi');
      msg = msg.replace(regex, nombreJugador);
    }

    if (j2 && j2.personaje) {
      const nombrePersonaje = j2.personaje.nombre;
      const nombreJugador = j2.nombre;
      const regex = new RegExp(nombrePersonaje, 'gi');
      msg = msg.replace(regex, nombreJugador);
    }

    return msg;
  }
}
