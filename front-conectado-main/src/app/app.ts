import { Component, inject, signal, effect } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from './services/auth.service';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  authService = inject(AuthService);
  router = inject(Router);
  
  protected readonly title = signal('PracticaFinal');
  
  isLoggedIn = signal(false);
  currentRoute = signal('');

  constructor() {
    // Escuchar cambios de autenticación
    this.authService.user$.subscribe(user => {
      this.isLoggedIn.set(!!user);
    });

    // Escuchar cambios de ruta
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.currentRoute.set(event.urlAfterRedirects);
    });
  }

  get showMobileNav(): boolean {
    const hideOnRoutes = ['/login', '/registro', '/reset-password', '/partida', '/partida-activa', '/seleccion-personajes'];
    const isAuthRoute = hideOnRoutes.some(route => this.currentRoute().startsWith(route));
    return this.isLoggedIn() && !isAuthRoute;
  }

  logout() {
    localStorage.removeItem('game_active_session');
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
