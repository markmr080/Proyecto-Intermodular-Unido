import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, switchMap, BehaviorSubject, tap } from 'rxjs';

// -------------------------------------------------------
//  INTERFACES EXPORTADAS (usadas por Menu, Perfil, etc.)
// -------------------------------------------------------

export interface UserDB {
  email: string;
  username: string;
  profilePicture?: string;
}

export interface AuthResponse {
  token: string;
}

export interface StatsDTO {
  username: string;
  partidasJugadas: number;
  partidasGanadas: number;
  hitsAcertados: number;
  hitsFallados: number;
  barcosHundidos: number;
  punteria: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `http://${window.location.hostname}:8080/api/auth`;
  private readonly STATS_URL = `http://${window.location.hostname}:8080/api/estadisticas`;

  private userSubject = new BehaviorSubject<UserDB | undefined>(undefined);
  public user$ = this.userSubject.asObservable();

  // TOKEN en sessionStorage: se borra al cerrar la pestaña
  private readonly TOKEN_KEY = 'auth_token';

  // Usuario en sessionStorage
  private readonly USER_KEY = 'current_user';
  private readonly PIC_KEY = 'current_user_pic';

  /**
   * El fingerprint se guarda SOLO en memoria (variable privada).
   * No se persiste en localStorage ni sessionStorage para que un ataque XSS
   * no pueda leerlo junto al token y usarlos combinados.
   * Al recargar la página se recalcula automáticamente.
   */
  private cachedFingerprint: string | null = null;

  private readonly MIDDLEWARE_CREDS = {
    username: 'middleware_admin',
    password: 'clave_secreta_del_middleware_2026'
  };

  constructor(private http: HttpClient) {
    this.refreshUser();
  }

  private refreshUser(): void {
    this.userSubject.next(this.getCurrentUser());
  }

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

    let hashHex = '';
    
    // crypto.subtle SOLO está disponible en localhost o HTTPS (Secure Contexts).
    // Si se accede por IP en HTTP (ej. desde un móvil), crypto.subtle es undefined.
    if (crypto && crypto.subtle) {
      const encoder = new TextEncoder();
      const hashBuffer = await crypto.subtle.digest('SHA-256', encoder.encode(datos));
      hashHex = Array.from(new Uint8Array(hashBuffer))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('');
    } else {
      // Fallback simple para HTTP inseguro (sin HTTPS)
      let hash = 0;
      for (let i = 0; i < datos.length; i++) {
        const char = datos.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // Convert to 32bit integer
      }
      hashHex = Math.abs(hash).toString(16).padStart(8, '0');
    }

    // Solo en memoria — NO en sessionStorage ni localStorage
    this.cachedFingerprint = hashHex;
    return hashHex;
  }

  /** Devuelve el fingerprint cacheado en memoria (síncrono). Null si aún no se calculó. */
  getFingerprintSync(): string | null {
    return this.cachedFingerprint;
  }

  // -------------------------------------------------------
  //  HELPER PRIVADO: obtener token de middleware con fingerprint
  //  Centraliza la lógica repetida en todos los métodos protegidos
  // -------------------------------------------------------

  private withMiddlewareToken<T>(
    fn: (token: string, fp: string) => Observable<T>
  ): Observable<T> {
    return from(this.generarFingerprint()).pipe(
      switchMap(fp =>
        new Observable<T>(observer => {
          this.http.post<AuthResponse>(`${this.API_URL}/login`, this.MIDDLEWARE_CREDS, {
            headers: { 'X-Fingerprint': fp }
          }).subscribe({
            next: (res) => {
              fn(res.token, fp).subscribe({
                next: (val) => { observer.next(val); observer.complete(); },
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
  //  GESTIÓN DEL TOKEN — sessionStorage
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
    sessionStorage.removeItem(this.PIC_KEY);
    this.cachedFingerprint = null; // Limpiamos también la memoria
    this.refreshUser();
  }

  // -------------------------------------------------------
  //  REGISTRO — el token de middleware NO se guarda como sesión
  // -------------------------------------------------------

  register(username: string, email: string, password: string): Observable<any> {
    const body = { username, email, password };
    return this.withMiddlewareToken((token, fp) =>
      this.http.post(`${this.API_URL}/register`, body, {
        headers: { 'Authorization': `Bearer ${token}`, 'X-Fingerprint': fp }
      })
    );
  }

  // -------------------------------------------------------
  //  LOGIN — genera fingerprint, obtiene token y lo guarda en sessionStorage
  // -------------------------------------------------------

  login(username: string, password: string): Observable<any> {
    const body = { username, password };

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
                next: (valRes: any) => {
                  sessionStorage.setItem(this.USER_KEY, username);
                  if (valRes.profilePicture) {
                    sessionStorage.setItem(this.PIC_KEY, valRes.profilePicture);
                  }
                  this.refreshUser();
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
  //  RECUPERACIÓN / RESET DE CONTRASEÑA
  // -------------------------------------------------------

  forgotPassword(email: string): Observable<any> {
    return this.withMiddlewareToken((token, fp) =>
      this.http.post(`${this.API_URL}/forgot-password`, { email }, {
        headers: { 'Authorization': `Bearer ${token}`, 'X-Fingerprint': fp }
      })
    );
  }

  resetPasswordWithToken(resetToken: string, newPassword: string): Observable<any> {
    return this.withMiddlewareToken((token, fp) =>
      this.http.post(`${this.API_URL}/reset-password`, { token: resetToken, newPassword }, {
        headers: { 'Authorization': `Bearer ${token}`, 'X-Fingerprint': fp }
      })
    );
  }

  // -------------------------------------------------------
  //  ACTUALIZACIÓN DE DATOS DE USUARIO
  // -------------------------------------------------------

  /**
   * Cambia la contraseña de un usuario en el backend.
   */
  updatePassword(username: string, newPassword: string): Observable<any> {
    return this.withMiddlewareToken((token, fp) =>
      this.http.post(`${this.API_URL}/update-password`, { username, newPassword }, {
        headers: { 'Authorization': `Bearer ${token}`, 'X-Fingerprint': fp }
      })
    );
  }

  /**
   * Cambia el nickname de un usuario en el backend.
   */
  updateNickname(currentUsername: string, newUsername: string): Observable<any> {
    return this.withMiddlewareToken((token, fp) =>
      this.http.post(`${this.API_URL}/update-nickname`, { currentUsername, newUsername }, {
        headers: { 'Authorization': `Bearer ${token}`, 'X-Fingerprint': fp }
      })
    );
  }

  /**
   * Actualiza el nombre de usuario almacenado en sessionStorage
   * (se llama después de un updateNickname exitoso).
   */
  updateUsername(newName: string): void {
    sessionStorage.setItem(this.USER_KEY, newName);
    this.refreshUser();
  }

  /**
   * Actualiza la foto de perfil en el backend y en sessionStorage.
   */
  updateProfilePicture(username: string, url: string): Observable<any> {
    sessionStorage.setItem(this.PIC_KEY, url);
    this.refreshUser();
    return this.withMiddlewareToken((token, fp) =>
      this.http.post(`${this.API_URL}/update-profile-picture`, { username, profilePicture: url }, {
        headers: { 'Authorization': `Bearer ${token}`, 'X-Fingerprint': fp }
      })
    );
  }

  // -------------------------------------------------------
  //  ESTADÍSTICAS — consulta a MySQL vía backend
  // -------------------------------------------------------

  /**
   * Obtiene las estadísticas del jugador desde el backend (MySQL).
   * Devuelve un Observable<StatsDTO> para que el componente pueda
   * reaccionar cuando lleguen los datos.
   */
  getUserStats(username: string): Observable<StatsDTO> {
    return this.withMiddlewareToken((token, fp) =>
      this.http.get<StatsDTO>(`${this.STATS_URL}/jugador/${username}`, {
        headers: { 'Authorization': `Bearer ${token}`, 'X-Fingerprint': fp }
      })
    );
  }

  // -------------------------------------------------------
  //  MÉTODOS DE SESIÓN ACTUALES
  // -------------------------------------------------------

  getCurrentUsername(): string {
    return sessionStorage.getItem(this.USER_KEY) ?? 'Invitado';
  }

  getCurrentUser(): UserDB | undefined {
    if (!this.isLoggedIn()) return undefined;
    const username = this.getCurrentUsername();
    const storedPic = sessionStorage.getItem(this.PIC_KEY);

    return {
      username,
      email: 'usuario@ejemplo.com',
      profilePicture: storedPic ?? ('https://api.dicebear.com/7.x/adventurer/svg?seed=' + username)
    };
  }
}