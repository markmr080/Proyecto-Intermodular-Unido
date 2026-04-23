import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { tap, from, switchMap } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Usamos 'from' para manejar la promesa del fingerprint de forma reactiva
  return from(authService.generarFingerprint()).pipe(
    switchMap(fingerprint => {
      const token = authService.getToken();
      let authReq = req;
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

      return next(authReq).pipe(
        tap(event => {
          if (event instanceof HttpResponse) {
            const newToken = event.headers.get('Token-Nuevo');
            if (newToken) {
              console.log('🔄 ¡Detectado Token nuevo! Renovando sesión...');
              authService.saveToken(newToken);
            }
          }
        })
      );
    })
  );
};