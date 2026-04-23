import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service'; // Ajusta la ruta
import { tap } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // 1. A LA IDA: Si tenemos token, lo inyectamos en la cabecera junto al fingerprint
  let authReq = req;
  const fingerprint = authService.getFingerprintSync();
  const headers: Record<string, string> = {};

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (fingerprint) {
    headers['X-Fingerprint'] = fingerprint;
  }

  if (Object.keys(headers).length > 0) {
    authReq = req.clone({ setHeaders: headers });
  }

  // 2. A LA VUELTA: Escuchamos la respuesta del servidor
  return next(authReq).pipe(
    tap(event => {
      if (event instanceof HttpResponse) {
        // Buscamos la cabecera que programamos en Spring Boot
        const newToken = event.headers.get('Token-Nuevo');

        if (newToken) {
          console.log('🔄 ¡Detectado Token nuevo! Renovando sesión...');
          authService.saveToken(newToken);
        }
      }
    })
  );
};