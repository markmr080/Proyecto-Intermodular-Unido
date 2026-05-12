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
  private readonly SESSION_KEY = 'game_active_session';

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
      name: 'Aislinn', fleet: [5, 4, 3, 2, 2], img: 'https://static.wikia.nocookie.net/warhammerfb/images/8/8c/AislinnTWWIII1.jpg/revision/latest/scale-to-width-down/1200?cb=20251107155847',
      abilities: [
        { name: 'Señor del Mar Alto Elfo', desc: '20% probabilidad de ignorar escudos y protecciones.', type: 'passive', icon: '/imagenes/aislinn_habilidades/pasiva.png' },
        { name: 'Corte de Lothern', desc: 'Dos disparos independientes en dos casillas separadas.', type: 'offensive', icon: '/imagenes/aislinn_habilidades/ofensiva1.png' },
        { name: 'Ira de Mathlann', desc: 'Golpea en forma de cruz (5 casillas).', type: 'offensive', icon: '/imagenes/aislinn_habilidades/ofensiva2.png' },
        { name: 'Bruma Marina', desc: 'Oculta 4 casillas propias aleatorias.', type: 'defensive', icon: '/imagenes/aislinn_habilidades/defensiva.png' }
      ]
    },
    {
      name: 'Lokhir', fleet: [5, 3, 3, 2, 2], img: 'https://static.wikia.nocookie.net/labibliotecadelviejomundo/images/9/94/Lokhir_Fellheart_Octava.jpg/revision/latest?cb=20171008101822&path-prefix=es',
      abilities: [
        { name: 'Saqueador Especialista', desc: 'Al hundir un barco, revela una casilla del siguiente.', type: 'passive', icon: '/imagenes/lokhir_habilidades/pasiva.png' },
        { name: 'Andanada Druchii', desc: 'Dispara a 3 casillas en diagonal.', type: 'offensive', icon: '/imagenes/lokhir_habilidades/ofensiva1.png' },
        { name: 'Furia Corsaria', desc: 'Bengalas en área 3x3. Revela barcos sin causar daño.', type: 'offensive', icon: '/imagenes/lokhir_habilidades/ofensiva2.png' },
        { name: 'Yelmo del Kraken', desc: 'Reubica uno de tus barcos enteros.', type: 'defensive', icon: '/imagenes/lokhir_habilidades/defensiva.png' }
      ]
    },
    {
      name: 'Aranessa', fleet: [4, 4, 3, 3, 2], img: 'https://cdnb.artstation.com/p/assets/covers/images/030/971/581/large/mauro-matheus-mauro-matheus-aranessathumb.jpg?1602188142',
      abilities: [
        { name: 'Tripulación de los Muertos', desc: '20% de probabilidad de ignorar el daño recibido.', type: 'passive', icon: '/imagenes/aranessa_habilidades/pasiva.png' },
        { name: 'Pólvora Vampírica', desc: 'El fuego se propaga en cruz si impacta un barco.', type: 'offensive', icon: '/imagenes/aranessa_habilidades/ofensiva1.png' },
        { name: 'Disparo de Saloma', desc: 'Destruye forzosamente nieblas o escudos del tablero rival.', type: 'offensive', icon: '/imagenes/aranessa_habilidades/ofensiva2.png' },
        { name: 'Hija de Stromfels', desc: 'Escudo total: el próximo disparo enemigo falla automáticamente.', type: 'defensive', icon: '/imagenes/aranessa_habilidades/defensiva.png' }
      ]
    }
  ];

  constructor() {
    this.authService.user$.subscribe(user => {
      this.currentUser = user;
    });
  }

  ngOnInit(): void {
    // Comprobar si hay una sesión de partida activa pendiente de reconexion
    try {
      const raw = localStorage.getItem(this.SESSION_KEY);
      if (raw) {
        const session = JSON.parse(raw);
        const username = this.authService.getCurrentUsername();
        if (session.roomCode && session.username === username) {
          // VERIFICACIÓN: Comprobar con el servidor si la sala sigue activa
          this.roomService.isSalaActiva(session.roomCode).subscribe({
            next: (resp) => {
              if (resp.activa) {
                this.roomCodeReconexion = session.roomCode;
                // Pequeño delay para que el menú se pinte primero
                setTimeout(() => {
                  this.mostrarPopupReconexion = true;
                }, 600);
              } else {
                // La sala ya no existe o no está activa -> limpiar
                localStorage.removeItem(this.SESSION_KEY);
              }
            },
            error: () => {
              // Si hay error (ej. servidor caído), mejor no mostrar nada
              localStorage.removeItem(this.SESSION_KEY);
            }
          });
        }
      }
    } catch (e) { /* ignorar */ }

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
    
    // Conectar al socket para poder recibir el evento anterior
    this.socketService.connect();
    const user = this.authService.getCurrentUser();
    if (user) {
      this.socketService.registrarUsuario(user.username);
    }
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
