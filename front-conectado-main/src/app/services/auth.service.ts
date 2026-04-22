import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, of } from 'rxjs';

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
  private readonly TOKEN_KEY = 'auth_token';
  private readonly USER_KEY = 'current_user';
  private readonly MIDDLEWARE_CREDS = {
    nickname: 'middleware_admin',
    password: 'clave_secreta_del_middleware_2026'
  };

  constructor(private http: HttpClient) { }

  // --- GESTIÓN DEL TOKEN ---
  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken() && !!localStorage.getItem(this.USER_KEY);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  // --- REGISTRO Y LOGIN (BACKEND REAL) ---
  register(username: string, email: string, password: string): Observable<any> {
    const body = { nickname: username, email, password };
    
    // Primero obtenemos token de middleware, luego registramos
    return new Observable(observer => {
      this.http.post<AuthResponse>(`${this.API_URL}/login`, this.MIDDLEWARE_CREDS).subscribe({
        next: (res) => {
          if (res.token) this.saveToken(res.token);
          this.http.post(`${this.API_URL}/register`, body, {
            headers: { 'Authorization': `Bearer ${res.token}` }
          }).subscribe({
            next: (regRes) => { observer.next(regRes); observer.complete(); },
            error: (err) => observer.error(err)
          });
        },
        error: (err) => observer.error(err)
      });
    });
  }

  login(username: string, password: string): Observable<any> {
    const body = { nickname: username, password };
    
    // Primero nos logueamos como middleware, luego validamos al usuario final
    return new Observable(observer => {
      this.http.post<AuthResponse>(`${this.API_URL}/login`, this.MIDDLEWARE_CREDS).subscribe({
        next: (res) => {
          if (res.token) this.saveToken(res.token);
          // Validar el usuario real
          this.http.post(`${this.API_URL}/validate-user`, body, {
            headers: { 'Authorization': `Bearer ${res.token}` }
          }).subscribe({
            next: (valRes) => { 
              localStorage.setItem(this.USER_KEY, username);
              observer.next(valRes); 
              observer.complete(); 
            },
            error: (err) => observer.error(err)
          });
        },
        error: (err) => observer.error(err)
      });
    });
  }

  // --- MÉTODOS COMPATIBLES (PARA QUE NO EXPLOTE LA APP) ---

  getCurrentUsername(): string {
    const user = localStorage.getItem(this.USER_KEY);
    if (user) return user;
    return 'Invitado';
  }

  // Restauramos getCurrentUser() para que Menu y Perfil funcionen
  getCurrentUser(): UserDB | undefined {
    if (!this.isLoggedIn()) return undefined;
    return {
      username: this.getCurrentUsername(),
      email: 'usuario@ejemplo.com', // Temporal hasta que tengamos endpoint de perfil
      profilePicture: 'https://api.dicebear.com/7.x/adventurer/svg?seed=' + this.getCurrentUsername()
    };
  }

  // Añadimos resetPassword para el Login
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

  // Stubs para el Perfil
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