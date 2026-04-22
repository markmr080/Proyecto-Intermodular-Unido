import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService, UserDB } from '../services/auth.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css'
})
export class Perfil implements OnInit {
  authService = inject(AuthService);
  router = inject(Router);

  currentUser: UserDB | undefined;
  stats: any;

  // Estados de Modales
  showNameModal = false;
  showPassModal = false;

  // Campos de edición
  editName = '';
  editPass = '';

  availableAvatars = [
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Thor',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Odin',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Loki',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Freya',
    'https://api.dicebear.com/7.x/adventurer/svg?seed=Fenrir'
  ];

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
    if (!this.currentUser) {
      this.router.navigate(['/login']);
    }
    this.stats = this.authService.getUserStats();
  }

  cambiarAvatar(url: string) {
    if (this.currentUser) {
      this.authService.updateProfilePicture(url);
      this.currentUser.profilePicture = url;
    }
  }

  // --- Lógica de Modales ---

  abrirModalNombre() {
    this.editName = this.currentUser?.username || '';
    this.showNameModal = true;
  }

  confirmarNombre() {
    if (this.editName && this.editName.trim()) {
      this.authService.updateUsername(this.editName.trim());
      if (this.currentUser) {
        this.currentUser.username = this.editName.trim();
      }
      this.cerrarModales();
    }
  }

  abrirModalPass() {
    this.editPass = '';
    this.showPassModal = true;
  }

  confirmarPass() {
    if (this.editPass && this.editPass.length >= 6) {
      const username = this.currentUser?.username;
      if (username) {
        this.authService.updatePassword(username, this.editPass).subscribe({
          next: () => {
            this.cerrarModales();
            alert('Contraseña actualizada correctamente.');
          },
          error: (err) => {
            alert('Error al actualizar la contraseña: ' + (err.error?.message || err.message));
          }
        });
      }
    } else {
      alert('La contraseña debe tener al menos 6 caracteres.');
    }
  }

  cerrarModales() {
    this.showNameModal = false;
    this.showPassModal = false;
    this.editName = '';
    this.editPass = '';
  }

  regresar() {
    this.router.navigate(['/menu']);
  }
}


