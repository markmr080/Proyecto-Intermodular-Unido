import { Component, inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  authService = inject(AuthService);
  router = inject(Router);
  route = inject(ActivatedRoute);

  resetForm = new FormGroup({
    newPassword: new FormControl('', [Validators.required, Validators.minLength(6)])
  });

  token: string | null = null;
  isSubmitted = false;
  message = '';
  isSuccess = false;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
      if (!this.token) {
        this.message = 'Token inválido o no proporcionado.';
      }
    });
  }

  onSubmit() {
    this.isSubmitted = true;
    this.message = '';

    if (this.resetForm.valid && this.token) {
      const newPassword = this.resetForm.value.newPassword!;
      
      this.authService.resetPasswordWithToken(this.token, newPassword).subscribe({
        next: () => {
          this.isSuccess = true;
          this.message = '¡Contraseña actualizada con éxito! Puedes iniciar sesión.';
          setTimeout(() => this.router.navigate(['/login']), 3000);
        },
        error: (err) => {
          if (err.error === 'TOKEN_INVALIDO') {
            this.message = 'El enlace ha caducado o es inválido.';
          } else {
            this.message = 'Ocurrió un error al actualizar la contraseña.';
          }
        }
      });
    }
  }
}
