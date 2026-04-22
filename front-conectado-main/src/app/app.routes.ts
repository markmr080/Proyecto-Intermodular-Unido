import { Routes } from '@angular/router';

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
    loadComponent: () => import('./menu/menu.component').then(m => m.MenuComponent)
  },
  {
    path: 'lista-salas',
    loadComponent: () => import('./lista-salas/lista-salas').then(m => m.ListaSalas)
  },
  {
    path: 'partida/:code',
    loadComponent: () => import('./partida/partida').then(m => m.Partida)
  },

  {
    path: 'perfil',
    loadComponent: () => import('./perfil/perfil').then(m => m.Perfil)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];
