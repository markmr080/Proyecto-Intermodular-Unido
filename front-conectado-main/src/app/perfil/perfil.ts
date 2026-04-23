import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService, UserDB, StatsDTO } from '../services/auth.service';

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
  stats: StatsDTO = {
    nickname: '',
    partidasGanadas: 0,
    impactosAcertados: 0,
    impactosFallados: 0,
    punteria: '0%'
  };

  // Estados de Modales
  showNameModal = false;
  showPassModal = false;

  // Campos de edición
  editName = '';
  editPass = '';
  editPassConfirm = '';

  // UX: loading y mensajes
  isLoadingName = false;
  isLoadingPass = false;
  nameError = '';
  nameSuccess = '';
  passError = '';
  passSuccess = '';
  showPassword = false;
  showPasswordConfirm = false;

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
      return;
    }

    // Cargamos las estadísticas reales desde MySQL
    const nickname = this.currentUser.username;
    this.authService.getUserStats(nickname).subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (err) => {
        // Si el backend falla (ej. apagado), mantenemos los ceros para no romper la UI
        console.warn('No se pudieron cargar las estadísticas:', err);
      }
    });
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
    this.nameError = '';
    this.nameSuccess = '';
    this.showNameModal = true;
  }

  confirmarNombre() {
    this.nameError = '';
    this.nameSuccess = '';

    const nuevoNombre = this.editName.trim();
    if (!nuevoNombre || nuevoNombre.length < 3) {
      this.nameError = 'El nombre debe tener al menos 3 caracteres.';
      return;
    }

    if (nuevoNombre === this.currentUser?.username) {
      this.nameError = 'El nuevo nombre debe ser diferente al actual.';
      return;
    }

    const nombreActual = this.currentUser?.username;
    if (!nombreActual) return;

    this.isLoadingName = true;
    this.authService.updateNickname(nombreActual, nuevoNombre).subscribe({
      next: () => {
        this.isLoadingName = false;
        // Actualizar sesión local
        this.authService.updateUsername(nuevoNombre);
        if (this.currentUser) {
          this.currentUser.username = nuevoNombre;
        }
        this.nameSuccess = '¡Nombre actualizado correctamente!';
        setTimeout(() => this.cerrarModales(), 1500);
      },
      error: (err) => {
        this.isLoadingName = false;
        const msg = err.error?.message || err.message || '';
        if (msg.includes('NICKNAME_DUPLICADO')) {
          this.nameError = 'Ese nombre ya está en uso. Elige otro.';
        } else {
          this.nameError = 'Error al actualizar el nombre. Inténtalo de nuevo.';
        }
      }
    });
  }

  abrirModalPass() {
    this.editPass = '';
    this.editPassConfirm = '';
    this.passError = '';
    this.passSuccess = '';
    this.showPassword = false;
    this.showPasswordConfirm = false;
    this.showPassModal = true;
  }

  confirmarPass() {
    this.passError = '';
    this.passSuccess = '';

    if (!this.editPass || this.editPass.length < 6) {
      this.passError = 'La contraseña debe tener al menos 6 caracteres.';
      return;
    }

    if (this.editPass !== this.editPassConfirm) {
      this.passError = 'Las contraseñas no coinciden.';
      return;
    }

    const username = this.currentUser?.username;
    if (!username) return;

    this.isLoadingPass = true;
    this.authService.updatePassword(username, this.editPass).subscribe({
      next: () => {
        this.isLoadingPass = false;
        this.passSuccess = '¡Contraseña actualizada correctamente!';
        setTimeout(() => this.cerrarModales(), 1500);
      },
      error: (err) => {
        this.isLoadingPass = false;
        this.passError = 'Error al actualizar la contraseña: ' + (err.error?.message || err.message || 'Inténtalo de nuevo.');
      }
    });
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  togglePasswordConfirmVisibility() {
    this.showPasswordConfirm = !this.showPasswordConfirm;
  }

  cerrarModales() {
    this.showNameModal = false;
    this.showPassModal = false;
    this.editName = '';
    this.editPass = '';
    this.editPassConfirm = '';
    this.nameError = '';
    this.nameSuccess = '';
    this.passError = '';
    this.passSuccess = '';
    this.isLoadingName = false;
    this.isLoadingPass = false;
  }

  regresar() {
    this.router.navigate(['/menu']);
  }
}
