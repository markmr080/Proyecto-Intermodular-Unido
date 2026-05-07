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
  flota: PersonajeFlota[];
}

@Injectable({
  providedIn: 'root'
})
export class PersonajeService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/personajes';

  getPersonajes(): Observable<PersonajeBackend[]> {
    return this.http.get<PersonajeBackend[]>(this.apiUrl);
  }
}
