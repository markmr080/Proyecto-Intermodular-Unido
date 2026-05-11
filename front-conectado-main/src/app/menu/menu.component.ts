import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService, UserDB } from '../services/auth.service';
@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css'
})
export class MenuComponent {
  router = inject(Router);
  authService = inject(AuthService);

  currentUser: UserDB | undefined;

  showPlayModal = false;
  playMode: 'select' | 'join' = 'select'; // 'select' para elegir crear/unir, 'join' para meter código
  roomCode = '';

  showProfileModal = false;

  currentMobileIndex = 0;

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
      name: 'Wulfrik', atk: 95, hp: 120, 
      img: 'https://www.reddit.com/r/totalwar/comments/b76m9o/wulfrik_the_wanderer_by_inna_petyakina/?tl=es-419',
      localImg: '/imagenes/wulfrik.jpg',
      abilities: [
        { name: 'Cazador de Naves', desc: 'Si aciertas a un barco enemigo, ganas un disparo extra en este turno inmediatamente.', type: 'passive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Hunter&backgroundColor=1A1512' },
        { name: 'Desafío del Errante', desc: 'Disparo preciso que de impactar en agua, fuerza al rival a revelar la posición aleatoria de un barco.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Espada&backgroundColor=1A1512' },
        { name: 'Colmillo de los Mares', desc: 'Impacta un área en línea horizontal abarcando 3 casillas consecutivas.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Barco&backgroundColor=1A1512' },
        { name: 'Favor Ruinoso', desc: 'Escuda una de tus casillas. Si el enemigo dispara ahí en el próximo turno, fallará automáticamente.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Piel&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Aislinn', atk: 90, hp: 100, 
      img: 'https://static.wikia.nocookie.net/warhammerfb/images/8/8c/AislinnTWWIII1.jpg/revision/latest/scale-to-width-down/1200?cb=20251107155847',
      localImg: '/imagenes/aislinn.jpg',
      abilities: [
        { name: 'Señor del Mar Alto Elfo', desc: 'Tus ataques tácticos tienen un 15% de probabilidad de ignorar los escudos o niebla enemiga.', type: 'passive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Tide&backgroundColor=1A1512' },
        { name: 'Corte de Lothern', desc: 'Realiza dos disparos independientes en dos casillas separadas a tu elección.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Lanza&backgroundColor=1A1512' },
        { name: 'Ira de Mathlann', desc: 'Golpea en forma de cruz (5 casillas), dañando gravemente formaciones navales juntas.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Tormenta&backgroundColor=1A1512' },
        { name: 'Bruma Marina', desc: 'Oculta tus casillas en un área de 2x2. Si el enemigo acierta allí, el golpe se anula.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Niebla&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Lokhir', atk: 85, hp: 110, 
      img: 'https://static.wikia.nocookie.net/labibliotecadelviejomundo/images/9/94/Lokhir_Fellheart_Octava.jpg/revision/latest?cb=20171008101822&path-prefix=es',
      localImg: '/imagenes/lokhir.webp',
      abilities: [
        { name: 'Saqueador Especialista', desc: 'Al hundir completamente un barco enemigo, se revela automáticamente una casilla del siguiente.', type: 'passive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Raid&backgroundColor=1A1512' },
        { name: 'Andanada Druchii', desc: 'Dispara a 3 casillas consecutivas pero en posición diagonal.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Daga&backgroundColor=1A1512' },
        { name: 'Furia Corsaria', desc: 'Lanza bengalas sobre un área de 3x3. Revela barcos enemigos sin causarles daño.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Flechas&backgroundColor=1A1512' },
        { name: 'Yelmo del Kraken', desc: 'Te permite reubicar uno de tus barcos enteros a una nueva posición vacía.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Tentaculo&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Aranessa', atk: 100, hp: 105, 
      img: 'https://cdnb.artstation.com/p/assets/covers/images/030/971/581/large/mauro-matheus-mauro-matheus-aranessathumb.jpg?1602188142',
      localImg: '/imagenes/Aranessa (1).jpg',
      abilities: [
        { name: 'Casco Reforzado', desc: 'El barco más pequeño de tu flota requiere de dos impactos en lugar de uno para hundirse.', type: 'passive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Queen&backgroundColor=1A1512' },
        { name: 'Pólvora Vampírica', desc: 'Elige una casilla. Si impacta en un barco, su fuego se propaga extendiéndose en área 2x2.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Sable&backgroundColor=1A1512' },
        { name: 'Disparo de Saloma', desc: 'Dispara con la legendaria Reina Bess, destruyendo forzosamente nieblas o escudos del tablero rival.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Explosion&backgroundColor=1A1512' },
        { name: 'Hija de Stromfels', desc: 'Invoca un remolino mágico. Anula por un turno completo cualquier disparo que intente dañar a tu flota.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Tiburon&backgroundColor=1A1512' }
      ]
    }
  ];

  constructor() {
    this.authService.user$.subscribe(user => {
      this.currentUser = user;
      if (!user && this.router.url !== '/login') {
        // Opcional: solo redirigir si no estamos ya en login
        // this.router.navigate(['/login']);
      }
    });
  }

  logout() {
    this.authService.logout();   // borra token, usuario y fingerprint de sessionStorage
    this.router.navigate(['/login']);
  }

  abrirModalPerfil() {
    this.router.navigate(['/perfil']);
  }

  abrirModalJugar() {
    this.router.navigate(['/lista-salas']);
  }
}
