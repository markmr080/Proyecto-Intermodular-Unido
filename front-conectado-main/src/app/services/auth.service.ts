import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';

// RE-AÑADIMOS LA INTERFAZ: Los componentes como Menu y Perfil la necesitan para compilar
export interface UserDB {
  email: string;
  username: string;
  profilePicture?: string;
}

export interface AuthResponse {
  token: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/auth';

  // TOKEN en sessionStorage: se borra al cerrar la pestaña
  private readonly TOKEN_KEY = 'auth_token';

  // Usuario en sessionStorage
  private readonly USER_KEY = 'current_user';

  /**
   * El fingerprint se guarda SOLO en memoria (variable privada).
   * No se persiste en localStorage ni sessionStorage para que un ataque XSS
   * no pueda leerlo junto al token y usarlos combinados.
   * Al recargar la página se recalcula automáticamente.
   */
  private cachedFingerprint: string | null = null;

  private readonly MIDDLEWARE_CREDS = {
    nickname: 'middleware_admin',
    password: 'clave_secreta_del_middleware_2026'
  };

  constructor(private http: HttpClient) { }

  // -------------------------------------------------------
  //  FINGERPRINTING: huella del navegador (SHA-256)
  // -------------------------------------------------------

  async generarFingerprint(): Promise<string> {
    // Si ya está en memoria, lo devolvemos (no tocamos ningún storage)
    if (this.cachedFingerprint) return this.cachedFingerprint;

    const datos = [
      navigator.userAgent,
      navigator.language,
      screen.width + 'x' + screen.height,
      screen.colorDepth.toString(),
      Intl.DateTimeFormat().resolvedOptions().timeZone,
      navigator.hardwareConcurrency?.toString() ?? '0',
      navigator.platform ?? ''
    ].join('|');

    const encoder = new TextEncoder();
    const hashBuffer = await crypto.subtle.digest('SHA-256', encoder.encode(datos));
    const hashHex = Array.from(new Uint8Array(hashBuffer))
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');

    // Solo en memoria — NO en sessionStorage ni localStorage
    this.cachedFingerprint = hashHex;
    return hashHex;
  }

  /** Devuelve el fingerprint cacheado en memoria (síncrono). Null si aún no se calculó. */
  getFingerprintSync(): string | null {
    return this.cachedFingerprint;
  }

  // -------------------------------------------------------
  //  GESTIÓN DEL TOKEN — ahora en sessionStorage
  // -------------------------------------------------------

  saveToken(token: string): void {
    sessionStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return sessionStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken() && !!sessionStorage.getItem(this.USER_KEY);
  }

  /**
   * Cierra la sesión del usuario:
   * - Borra el token JWT de sessionStorage
   * - Borra el nombre de usuario
   * - Borra el fingerprint cacheado
   */
  logout(): void {
    sessionStorage.removeItem(this.TOKEN_KEY);
    sessionStorage.removeItem(this.USER_KEY);
    this.cachedFingerprint = null; // Limpiamos también la memoria
  }

  // -------------------------------------------------------
  //  REGISTRO — el token de middleware NO se guarda como sesión
  // -------------------------------------------------------

  register(username: string, email: string, password: string): Observable<any> {
    const body = { nickname: username, email, password };

    return from(this.generarFingerprint()).pipe(
      switchMap(fp =>
        new Observable(observer => {
          // Obtenemos token de middleware solo para autorizar la llamada
          this.http.post<AuthResponse>(`${this.API_URL}/login`, this.MIDDLEWARE_CREDS, {
            headers: { 'X-Fingerprint': fp }
          }).subscribe({
            next: (res) => {
              // NOTA: NO guardamos este token — es del middleware_admin, no del usuario
              this.http.post(`${this.API_URL}/register`, body, {
                headers: {
                  'Authorization': `Bearer ${res.token}`,
                  'X-Fingerprint': fp
                }
              }).subscribe({
                next: (regRes) => { observer.next(regRes); observer.complete(); },
                error: (err) => observer.error(err)
              });
            },
            error: (err) => observer.error(err)
          });
        })
      )
    );
  }

  // -------------------------------------------------------
  //  LOGIN — genera fingerprint, obtiene token y lo guarda en sessionStorage
  // -------------------------------------------------------

  login(username: string, password: string): Observable<any> {
    const body = { nickname: username, password };

    return from(this.generarFingerprint()).pipe(
      switchMap(fp =>
        new Observable(observer => {
          // 1. Login del middleware con fingerprint → obtenemos token firmado con "fp"
          this.http.post<AuthResponse>(`${this.API_URL}/login`, this.MIDDLEWARE_CREDS, {
            headers: { 'X-Fingerprint': fp }
          }).subscribe({
            next: (res) => {
              // Guardamos el token en sessionStorage (se borra al cerrar pestaña)
              this.saveToken(res.token);

              // 2. Validamos las credenciales del usuario real
              this.http.post(`${this.API_URL}/validate-user`, body, {
                headers: {
                  'Authorization': `Bearer ${res.token}`,
                  'X-Fingerprint': fp
                }
              }).subscribe({
                next: (valRes) => {
                  sessionStorage.setItem(this.USER_KEY, username);
                  observer.next(valRes);
                  observer.complete();
                },
                error: (err) => observer.error(err)
              });
            },
            error: (err) => observer.error(err)
          });
        })
      )
    );
  }

  // -------------------------------------------------------
  //  MÉTODOS COMPATIBLES
  // -------------------------------------------------------

  getCurrentUsername(): string {
    return sessionStorage.getItem(this.USER_KEY) ?? 'Invitado';
  }

  getCurrentUser(): UserDB | undefined {
    if (!this.isLoggedIn()) return undefined;
    return {
      username: this.getCurrentUsername(),
      email: 'usuario@ejemplo.com',
      profilePicture: 'https://api.dicebear.com/7.x/adventurer/svg?seed=' + this.getCurrentUsername()
    };
  }

  forgotPassword(email: string): Observable<any> {
    const body = { email };
    return new Observable(observer => {
      this.http.post<AuthResponse>(`${this.API_URL}/login`, this.MIDDLEWARE_CREDS).subscribe({
        next: (res) => {
          this.http.post(`${this.API_URL}/forgot-password`, body, {
            headers: { 'Authorization': `Bearer ${res.token}` }
          }).subscribe({
            next: (val) => { observer.next(val); observer.complete(); },
            error: (err) => observer.error(err)
          });
        },
        error: (err) => observer.error(err)
      });
    });
  }

  resetPasswordWithToken(token: string, newPassword: string): Observable<any> {
    const body = { token, newPassword };
    return new Observable(observer => {
      this.http.post<AuthResponse>(`${this.API_URL}/login`, this.MIDDLEWARE_CREDS).subscribe({
        next: (res) => {
          this.http.post(`${this.API_URL}/reset-password`, body, {
            headers: { 'Authorization': `Bearer ${res.token}` }
          }).subscribe({
            next: (val) => { observer.next(val); observer.complete(); },
            error: (err) => observer.error(err)
          });
        },
        error: (err) => observer.error(err)
      });
    });
  }

  updateProfilePicture(url: string): void { console.log('Update foto:', url); }
  updateUsername(newName: string): void { console.log('Update nombre:', newName); }

  updatePassword(nickname: string, newPassword: string): Observable<any> {
    const body = { nickname, newPassword };
    return new Observable(observer => {
      this.http.post<AuthResponse>(`${this.API_URL}/login`, this.MIDDLEWARE_CREDS).subscribe({
        next: (res) => {
          this.http.post(`${this.API_URL}/update-password`, body, {
            headers: { 'Authorization': `Bearer ${res.token}` }
          }).subscribe({
            next: (val) => { observer.next(val); observer.complete(); },
            error: (err) => observer.error(err)
          });
        },
        error: (err) => observer.error(err)
      });
    });
  }

  getUserStats() {
    return { partidasGanadas: 0, impactosAcertados: 0, impactosFallados: 0, punteria: '0%' };
  }
}