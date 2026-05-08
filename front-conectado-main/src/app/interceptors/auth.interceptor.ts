import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { from, switchMap, tap } from 'rxjs';

/**
 * Interceptor HTTP que añade automáticamente:
 *  - Authorization: Bearer <token> si el usuario tiene sesión activa.
 *  - X-Fingerprint: <huella> generada con SHA-256 del navegador.
 *
 * El fingerprint se genera de forma asíncrona la primera vez (si no está cacheado)
 * para evitar enviar null y que el JwtFilter bloquee la petición.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Generar fingerprint (asíncrono la primera vez; cacheado a partir de ahí)
  return from(authService.generarFingerprint()).pipe(
    switchMap(fingerprint => {
      const headers: Record<string, string> = {};

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      if (fingerprint) {
        headers['X-Fingerprint'] = fingerprint;
      }

      const authReq = Object.keys(headers).length > 0
        ? req.clone({ setHeaders: headers })
        : req;

      // Escuchar la respuesta para renovar el token si el servidor lo indica
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