import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'registro',
    loadComponent: () => import('./registro/registro.component').then(m => m.RegistroComponent)
  },
  {
    path: 'menu',
    canActivate: [authGuard],
    loadComponent: () => import('./menu/menu.component').then(m => m.MenuComponent)
  },
  {
    path: 'lista-salas',
    canActivate: [authGuard],
    loadComponent: () => import('./lista-salas/lista-salas').then(m => m.ListaSalas)
  },
  {
    path: 'partida/:code',
    canActivate: [authGuard],
    loadComponent: () => import('./partida/partida').then(m => m.Partida)
  },
  {
    path: 'partida-activa/:code',
    canActivate: [authGuard],
    loadComponent: () => import('./partida-activa/partida-activa.component').then(m => m.PartidaActivaComponent)
  },
  {
    path: 'seleccion-personajes',
    canActivate: [authGuard],
    loadComponent: () => import('./seleccion-personajes/seleccion-personajes.component').then(m => m.SeleccionPersonajesComponent)
  },

  {
    path: 'perfil',
    canActivate: [authGuard],
    loadComponent: () => import('./perfil/perfil').then(m => m.Perfil)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
