import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  authService = inject(AuthService);
  router = inject(Router);

  loginForm = new FormGroup({
    username: new FormControl('', Validators.required),
    password: new FormControl('', Validators.required)
  });

  isSubmitted = false;
  showForgotPasswordModal = false;
  loginError = '';
  
  recoveryEmail = new FormControl('', [Validators.required, Validators.email]);
  recoveryMessage = '';

  onSubmit() {
    this.isSubmitted = true;
    this.loginError = '';
    
    // Solo borramos el error de auth para que se vuelva a evaluar todo normal
    if (this.loginForm.controls.username.hasError('authError')) {
      this.loginForm.controls.username.updateValueAndValidity();
    }
    if (this.loginForm.controls.password.hasError('authError')) {
      this.loginForm.controls.password.updateValueAndValidity();
    }

    if (this.loginForm.valid) {
      const username = this.loginForm.value.username!;
      const password = this.loginForm.value.password!;
      
      // CAMBIO CLAVE: Usamos .subscribe() en lugar de await
      this.authService.login(username, password).subscribe({
        next: (response) => {
          console.log('Login OK:', response);
          // Al ser correcto, el servicio ya guardó el token. Vamos al menú.
          this.router.navigate(['/menu']);
        },
        error: (err) => {
          console.error('Error en login:', err);
          
          // Manejo de errores según la excepción devuelta por el backend
          if (err.error && typeof err.error === 'string') {
            if (err.error === 'USUARIO_NO_ENCONTRADO') {
              this.loginError = 'El usuario no existe';
              this.loginForm.controls.username.setErrors({'authError': true});
            } else if (err.error === 'PASSWORD_INCORRECTO') {
              this.loginError = 'La contraseña es incorrecta';
              this.loginForm.controls.password.setErrors({'authError': true});
            } else {
              this.loginError = err.error;
            }
          } else if (err.status === 401 || err.status === 403) {
            this.loginError = 'Usuario o contraseña incorrectos';
            this.loginForm.controls.password.setErrors({'authError': true});
          } else {
            this.loginError = 'Error de conexión con el servidor';
          }
        }
      });
    }
  }

  // --- Lógica del Modal (Se mantiene igual) ---

  openRecoveryModal(event: Event) {
    event.preventDefault();
    this.showForgotPasswordModal = true;
    this.recoveryMessage = '';
    this.recoveryEmail.reset();
  }

  closeRecoveryModal() {
    this.showForgotPasswordModal = false;
  }

  onModalClick(event: Event) {
    event.stopPropagation();
  }

  sendRecoveryEmail() {
    if (this.recoveryEmail.valid) {
      this.recoveryMessage = 'Enviando...';
      this.authService.forgotPassword(this.recoveryEmail.value!).subscribe({
        next: () => {
          this.recoveryMessage = '¡Correo de recuperación enviado con éxito!';
        },
        error: (err) => {
          if (err.error === 'EMAIL_NO_ENCONTRADO') {
            this.recoveryMessage = 'Este correo no está registrado.';
          } else {
            this.recoveryMessage = 'Error al enviar el correo.';
          }
        }
      });
    } else {
      this.recoveryMessage = 'Por favor pon un correo válido.';
    }
  }
}