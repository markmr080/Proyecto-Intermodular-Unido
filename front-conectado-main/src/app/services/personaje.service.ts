import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PersonajeFlota {
  tipoBarco: string;
  tamano: number;
  cantidad: number;
}

export interface PersonajeBackend {
  id: number;
  nombre: string;
  imagen: string;
  habilidadPasiva?: any;
  flota: PersonajeFlota[];
}

@Injectable({
  providedIn: 'root'
})
export class PersonajeService {
  private http = inject(HttpClient);
  private apiUrl = window.location.hostname === 'localhost' 
    ? 'http://localhost:8080/api/personajes' 
    : `https://${window.location.hostname}/api/personajes`;

  getPersonajes(): Observable<PersonajeBackend[]> {
    return this.http.get<PersonajeBackend[]>(this.apiUrl);
  }
}
