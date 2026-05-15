import { Component, inject } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const passwordsMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  if (password && confirmPassword && password !== confirmPassword) {
    return { passwordsMismatch: true };
  }
  return null;
};

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrl: './registro.component.css'
})
export class RegistroComponent {
  authService = inject(AuthService);
  router = inject(Router);

  registerForm = new FormGroup({
    username: new FormControl('', [
      Validators.required, 
      Validators.minLength(3), 
      Validators.maxLength(20),
      Validators.pattern('^[a-zA-Z0-9_]+$')
    ]),
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    confirmPassword: new FormControl('', Validators.required),
    edad: new FormControl('') // Lo dejamos aquí para que el HTML no de error, pero no lo usamos
  }, { validators: passwordsMatchValidator });

  isSubmitted = false;
  registerError = '';
  showPassword = false;
  showConfirmPassword = false;

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit() {
    this.isSubmitted = true;
    this.registerError = '';

    if (this.registerForm.valid) {
      const { username, email, password } = this.registerForm.value;
      
      // LLAMADA REAL AL BACKEND:
      // Usamos .subscribe() para "lanzar" la petición
      this.authService.register(username!, email!, password!).subscribe({
        next: (response) => {
          console.log('Registro exitoso', response);
          // Si todo va bien, mandamos al login para que entre
          this.router.navigate(['/login']);
        },
        error: (err) => {
          console.error('Error en el servidor', err);
          
          // Si el backend devuelve un mensaje de error específico (ej. EMAIL_DUPLICADO)
          if (err.error && err.error.message) {
            this.registerError = err.error.message;
          } else if (err.status === 409 || err.status === 400) {
            this.registerError = 'El nombre o el email ya están en uso.';
          } else {
            this.registerError = 'No se pudo completar el registro. Inténtalo de nuevo.';
          }
        }
      });
    }
  }

  cancelar() {
    this.router.navigate(['/login']);
  }
}