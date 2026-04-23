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

  availableAvatars = [
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Thor',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Odin',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Loki',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Freya',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Fenrir'
  ];

  characters = [
    {
      name: 'Thor', atk: 95, hp: 120, img: 'https://placehold.co/400x500/1A1512/B89158?text=Thor',
      abilities: [
        { name: 'Mjolnir', desc: 'Golpea con su martillo causando un gran daño eléctrico.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Mjolnir&backgroundColor=1A1512' },
        { name: 'Ira del Trueno', desc: 'Invoca un rayo que daña a los enemigos cercanos.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Trueno&backgroundColor=1A1512' },
        { name: 'Piel de Dios', desc: 'Se escuda con energía divina reduciendo el daño recibido.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Piel&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Odin', atk: 85, hp: 150, img: 'https://placehold.co/400x500/1A1512/B89158?text=Odin',
      abilities: [
        { name: 'Gungnir', desc: 'Arroja su lanza que nunca falla su objetivo.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Gungnir&backgroundColor=1A1512' },
        { name: 'Sabiduría de Mimir', desc: 'Anticipa los ataques enemigos, aumentando su evasión.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Sabiduria&backgroundColor=1A1512' },
        { name: 'Furia del Valhalla', desc: 'Aumenta temporalmente el ataque de todos los aliados.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Valhalla&backgroundColor=1A1512' },
        { name: 'Ojo Omnisciente', desc: 'Permite desvelar los movimientos ocultos del enemigo, protegiendo al equipo.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Ojo&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Loki', atk: 75, hp: 90, img: 'https://placehold.co/400x500/1A1512/B89158?text=Loki',
      abilities: [
        { name: 'Puñalada Trapera', desc: 'Ataca por la espalda causando daño crítico.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Daga&backgroundColor=1A1512' },
        { name: 'Ilusión', desc: 'Crea clones de sí mismo para confundir al enemigo.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Ilusion&backgroundColor=1A1512' },
        { name: 'Veneno de Serpiente', desc: 'Aplica veneno al objetivo causando daño continuo.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Veneno&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Freya', atk: 90, hp: 100, img: 'https://placehold.co/400x500/1A1512/B89158?text=Freya',
      abilities: [
        { name: 'Corte de Valkiria', desc: 'Realiza un corte rápido con su espada mágica.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Espada&backgroundColor=1A1512' },
        { name: 'Manto de Plumas', desc: 'Genera un escudo de plumas que bloquea un ataque.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Plumas&backgroundColor=1A1512' },
        { name: 'Luz de Folkvangr', desc: 'Ciega a los enemigos con una luz divina.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Luz&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Fenrir', atk: 110, hp: 80, img: 'https://placehold.co/400x500/1A1512/B89158?text=Fenrir',
      abilities: [
        { name: 'Mordisco Feroz', desc: 'Muerde a su presa causando daño masivo.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Mordisco&backgroundColor=1A1512' },
        { name: 'Aullido Aterrador', desc: 'Reduce la defensa y el ataque de los enemigos.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Aullido&backgroundColor=1A1512' },
        { name: 'Pelaje de Hierro', desc: 'Endurece su pelaje resistiendo ataques físicos.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Pelaje&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Heimdall', atk: 80, hp: 130, img: 'https://placehold.co/400x500/1A1512/B89158?text=Heimdall',
      abilities: [
        { name: 'Gjallarhorn', desc: 'Hace sonar su cuerno para alertar e invulnerabilizar aliados.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Cuerno&backgroundColor=1A1512' },
        { name: 'Visión Absoluta', desc: 'Detecta y expone el punto débil del enemigo.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Vision&backgroundColor=1A1512' },
        { name: 'Filo Bifrost', desc: 'Corta la realidad, desterrando parte de la energía del rival.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Bifrost&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Tyr', atk: 100, hp: 110, img: 'https://placehold.co/400x500/1A1512/B89158?text=Tyr',
      abilities: [
        { name: 'Justicia Ciega', desc: 'Golpea con furia impartiendo daño aumentado según la vida que le falte.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Justicia&backgroundColor=1A1512' },
        { name: 'Sacrificio Honorífico', desc: 'Pierde salud para absorber un ataque letal destinado a un aliado.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Sangre&backgroundColor=1A1512' },
        { name: 'Golpe Implacable', desc: 'Golpea con fuerza bruta destrozando escudos enemigos.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Guantelete&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Hel', atk: 120, hp: 70, img: 'https://placehold.co/400x500/1A1512/B89158?text=Hel',
      abilities: [
        { name: 'Toque Fúnebre', desc: 'Merma el alma del oponente reduciendo su capacidad de daño.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Alma&backgroundColor=1A1512' },
        { name: 'Aura del Helheim', desc: 'Congela los ataques físicos mitigando el daño.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Congelar&backgroundColor=1A1512' },
        { name: 'Ejército Muerto', desc: 'Invoca guerreros caídos que golpean múltiples veces al objetivo.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Calavera&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Baldur', atk: 90, hp: 140, img: 'https://placehold.co/400x500/1A1512/B89158?text=Baldur',
      abilities: [
        { name: 'Luz Pura', desc: 'Deslumbra al enemigo reduciendo severamente su destreza en combate.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=LuzPura&backgroundColor=1A1512' },
        { name: 'Inmunidad Bélica', desc: 'Hace ignorar todo el daño recibido temporalmente.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Invulnerable&backgroundColor=1A1512' },
        { name: 'Golpe Solar', desc: 'Desata la energía solar concentrada quemando a su objetivo.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Sol&backgroundColor=1A1512' }
      ]
    },
    {
      name: 'Skadi', atk: 105, hp: 85, img: 'https://placehold.co/400x500/1A1512/B89158?text=Skadi',
      abilities: [
        { name: 'Flecha Gélida', desc: 'Dispara un virote de puro hielo que ralentiza e infringe daño agudo.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Flecha&backgroundColor=1A1512' },
        { name: 'Muro Helado', desc: 'Levanta un muro de viento y nieve gruesa que detiene proyectiles.', type: 'defensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Ventisca&backgroundColor=1A1512' },
        { name: 'Avalancha', desc: 'Baja las temperaturas enterrando y paralizando a los atacantes.', type: 'offensive', icon: 'https://api.dicebear.com/7.x/icons/svg?seed=Avalancha&backgroundColor=1A1512' }
      ]
    }
  ];

  constructor() {
    this.currentUser = this.authService.getCurrentUser();
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
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
