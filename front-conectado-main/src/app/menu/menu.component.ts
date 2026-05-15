import { Component, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService, UserDB } from '../services/auth.service';
import { SocketService } from '../services/socket.service';
import { RoomService } from '../services/room.service';
import { Subscription } from 'rxjs';
@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css'
})
export class MenuComponent implements OnInit {
  router = inject(Router);
  authService = inject(AuthService);
  socketService = inject(SocketService);
  roomService = inject(RoomService);
  private subscriptions = new Subscription();

  currentUser: UserDB | undefined;

  showPlayModal = false;
  playMode: 'select' | 'join' = 'select';
  roomCode = '';
  showProfileModal = false;
  currentMobileIndex = 0;

  // --- Popup de reconexión ---
  mostrarPopupReconexion = false;
  roomCodeReconexion = '';
  /** Clave de sesión por usuario para evitar conflictos en el mismo navegador. */
  private get SESSION_KEY(): string {
    return `game_active_session_${this.authService.getCurrentUsername()}`;
  }

  nextChar() {
    this.currentMobileIndex = (this.currentMobileIndex + 1) % this.characters.length;
  }

  prevChar() {
    this.currentMobileIndex = (this.currentMobileIndex - 1 + this.characters.length) % this.characters.length;
  }

  handleImageError(event: any, fallback: string) {
    (event.target as HTMLImageElement).src = fallback;
  }

  availableAvatars = [
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Thor',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Odin',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Loki',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Freya',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Fenrir'
  ];

  characters = [
    {
      name: 'Wulfrik', fleet: [5, 4, 3, 3, 2], img: 'https://i.redd.it/m4wl1apwe6p21.jpg',
      abilities: [
        { name: 'Cazador de Naves', desc: 'Si aciertas a un barco enemigo, ganas un disparo extra.', type: 'passive', icon: '/imagenes/wulfrik_habilidades/pasiva.png' },
        { name: 'Desafío del Errante', desc: 'Lanza un disparo; si fallas, se revela la posición aleatoria de un barco enemigo.', type: 'offensive', icon: '/imagenes/wulfrik_habilidades/ofensiva1.png' },
        { name: 'Colmillo de los Mares', desc: 'Impacta un área en línea horizontal de 3 casillas.', type: 'offensive', icon: '/imagenes/wulfrik_habilidades/ofensiva2.png' },
        { name: 'Favor Ruinoso', desc: 'Escuda una casilla propia aleatoria.', type: 'defensive', icon: '/imagenes/wulfrik_habilidades/defensiva.png' }
      ]
    },
    {
      name: 'Aislinn', fleet: [5, 4, 3, 2, 2], img: '/imagenes/aislinn.jpg',
      abilities: [
        { name: 'Señor del Mar Alto Elfo', desc: '20% probabilidad de ignorar escudos y protecciones.', type: 'passive', icon: '/imagenes/aislinn_habilidades/pasiva.png' },
        { name: 'Corte de Lothern', desc: 'Dos disparos independientes en dos casillas separadas.', type: 'offensive', icon: '/imagenes/aislinn_habilidades/ofensiva1.png' },
        { name: 'Ira de Mathlann', desc: 'Golpea en forma de cruz (5 casillas).', type: 'offensive', icon: '/imagenes/aislinn_habilidades/ofensiva2.png' },
        { name: 'Bruma Marina', desc: 'Despliega una niebla en un área 2x2 de tu tablero que protege tus barcos.', type: 'defensive', icon: '/imagenes/aislinn_habilidades/defensiva.png' }
      ]
    },
    {
      name: 'Lokhir', fleet: [5, 3, 3, 2, 2], img: '/imagenes/Lokhir.png',
      abilities: [
        { name: 'Saqueador Especialista', desc: 'Al hundir un barco, revela una casilla del siguiente.', type: 'passive', icon: '/imagenes/lokhir_habilidades/pasiva.png' },
        { name: 'Andanada Druchii', desc: 'Dispara a 3 casillas en diagonal.', type: 'offensive', icon: '/imagenes/lokhir_habilidades/ofensiva1.png' },
        { name: 'Furia Corsaria', desc: 'Bengalas en área 3x3. Revela barcos sin causar daño.', type: 'offensive', icon: '/imagenes/lokhir_habilidades/ofensiva2.png' },
        { name: 'Yelmo del Kraken', desc: 'Protege tu barco más grande (Arca Negra). El escudo desaparece por completo al primer impacto.', type: 'defensive', icon: '/imagenes/lokhir_habilidades/defensiva.png' },
        { name: 'Venganza de Karond Kar', desc: 'Habilidad oculta: se activa al perder el Arca Negra. Bombardeo de 5 disparos aleatorios.', type: 'offensive', icon: '/imagenes/lokhir_habilidades/ofensiva3.png' }
      ]
    },
    {
      name: 'Aranessa', fleet: [4, 4, 3, 3, 2], img: '/imagenes/Aranessa (2).png',
      abilities: [
        { name: 'Tripulación de los Muertos', desc: '20% de probabilidad de ignorar el daño recibido.', type: 'passive', icon: '/imagenes/aranessa_habilidades/pasiva.png' },
        { name: 'Pólvora Vampírica', desc: 'El fuego se propaga en cruz si impacta un barco.', type: 'offensive', icon: '/imagenes/aranessa_habilidades/ofensiva1.png' },
        { name: 'Disparo de Saloma', desc: 'Destruye forzosamente nieblas o escudos del tablero rival.', type: 'offensive', icon: '/imagenes/aranessa_habilidades/ofensiva2.png' },
        { name: 'Hija de Stromfels', desc: 'Escudo total: el próximo disparo enemigo falla automáticamente.', type: 'defensive', icon: '/imagenes/aranessa_habilidades/defensiva.png' }
      ]
    },
    {
      name: 'Ikit Claw', fleet: [4, 3, 3, 2, 2], img: '/imagenes/ikitclaw.jpg',
      abilities: [
        { name: 'Ingeniero Brujo de Skryre', desc: '20% de probabilidad de que una habilidad ofensiva no consuma enfriamiento.', type: 'passive', icon: '/imagenes/ikitclaw_habilidades/pasiva.png' },
        { name: 'Rayo de Piedra Bruja', desc: 'Lanza un rayo potente que impacta una casilla y revela las adyacentes en cruz.', type: 'offensive', icon: '/imagenes/ikitclaw_habilidades/ofensiva1.png' },
        { name: 'Cohete de Muerte', desc: 'Impacta un área masiva de 3x3 casillas.', type: 'offensive', icon: '/imagenes/ikitclaw_habilidades/ofensiva2.png' },
        { name: 'Escudo de Piedra Bruja', desc: 'Protege un área aleatoria de 2x2 casillas contra el próximo disparo.', type: 'defensive', icon: '/imagenes/ikitclaw_habilidades/defensiva.png' }
      ]
    },
    {
      name: 'Balthasar Gelt', fleet: [5, 4, 3, 3, 2], img: '/imagenes/gelt.png',
      abilities: [
        { name: 'Metalurgia Dorada', desc: 'Al impactar un barco, reduce el enfriamiento de una habilidad activa aleatoria en 1 turno.', type: 'passive', icon: '/imagenes/gelt_habilidades/pasiva.png' },
        { name: 'Transmutación de Plomo', desc: 'Convierte una zona 2x2 en oro: revela barcos y causa daño.', type: 'offensive', icon: '/imagenes/gelt_habilidades/ofensiva1.png' },
        { name: 'Lluvia de Metal', desc: 'Invoca una lluvia de proyectiles que impacta en 3 casillas aleatorias del tablero enemigo.', type: 'offensive', icon: '/imagenes/gelt_habilidades/ofensiva2.png' },
        { name: 'Cuerpo de Hierro', desc: 'Protege todas las casillas de tu barco más grande con escudos mágicos.', type: 'defensive', icon: '/imagenes/gelt_habilidades/defensiva.png' }
      ]
    }
  ];

  constructor() {
    this.authService.user$.subscribe(user => {
      this.currentUser = user;
    });
  }

  ngOnInit(): void {
    // Conectar al socket pasando el token para evitar rechazos
    const token = this.authService.getToken();
    console.log('[Menu] Iniciando conexión socket...');
    this.socketService.connect(token || undefined);

    const user = this.authService.getCurrentUser();
    if (user) {
      console.log('[Menu] Registrando usuario en socket:', user.username);
      this.socketService.registrarUsuario(user.username);
    }

    // Comprobar si hay una sesión de partida activa pendiente de reconexion
    // La clave es específica por usuario, así no hay conflictos entre jugadores en el mismo navegador
    try {
      const raw = localStorage.getItem(this.SESSION_KEY);
      console.log('[Menu] Comprobando localStorage:', this.SESSION_KEY, '=>', raw);
      if (raw) {
        const session = JSON.parse(raw);
        if (session.roomCode) {
          console.log('[Menu] Sesión encontrada. Verificando sala con servidor:', session.roomCode);
          // VERIFICACIÓN: Comprobar con el servidor si la sala sigue activa
          this.roomService.isSalaActiva(session.roomCode).subscribe({
            next: (resp) => {
              console.log('[Menu] Respuesta de isSalaActiva:', resp);
              if (resp.activa) {
                this.roomCodeReconexion = session.roomCode;
                setTimeout(() => {
                  console.log('[Menu] Mostrando popup de reconexión ahora.');
                  this.mostrarPopupReconexion = true;
                }, 600);
              } else {
                console.log('[Menu] La sala ya no está activa según el servidor.');
                localStorage.removeItem(this.SESSION_KEY);
              }
            },
            error: (err) => {
              console.error('[Menu] Error al verificar sala activa (se mantiene la sesión):', err);
            }
          });
        }
      }
    } catch (e) {
      console.error('[Menu] Error parseando sesión de localStorage:', e);
    }

    // Escuchar si la reconexión expira mientras estamos en el menú
    this.subscriptions.add(
      this.socketService.reconexionExpirada$.subscribe((roomCode) => {
        if (roomCode === this.roomCodeReconexion) {
          console.log('[Menu] La reconexión ha expirado. Cerrando popup.');
          this.mostrarPopupReconexion = false;
          localStorage.removeItem(this.SESSION_KEY);
        }
      })
    );

  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  reconectarAPartida(): void {
    // Verificar una última vez antes de navegar
    this.roomService.isSalaActiva(this.roomCodeReconexion).subscribe({
      next: (resp) => {
        if (resp.activa) {
          this.mostrarPopupReconexion = false;
          this.router.navigate(['/partida-activa', this.roomCodeReconexion]);
        } else {
          alert('La partida ya no está disponible (el tiempo de reconexión ha expirado).');
          this.descartarReconexion();
        }
      },
      error: () => {
        this.descartarReconexion();
      }
    });
  }

  descartarReconexion(): void {
    localStorage.removeItem(this.SESSION_KEY);
    this.mostrarPopupReconexion = false;
  }

  logout() {
    localStorage.removeItem(this.SESSION_KEY); // Limpiar sesión activa al salir
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  abrirModalPerfil() {
    this.router.navigate(['/perfil']);
  }

  abrirModalJugar() {
    this.router.navigate(['/lista-salas']);
  }

  /** Devuelve un array de longitud `n` para usar con @for en el template */
  range(n: number): number[] {
    return Array.from({ length: n }, (_, i) => i);
  }
}
