import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

export interface Room {
  id?: number;
  jugador1: string;
  nombreJugador1: string;
  avatarJugador1: string;
  jugador2?: string;
  nombreJugador2?: string;
  avatarJugador2?: string;
  estado: string; // 'ESPERANDO', 'EN_CURSO', 'FINALIZADA'
  codigoSala: string;
  turno?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RoomService {
  private readonly API_URL = 'http://localhost:8080/api/partidas';
  private http = inject(HttpClient);

  getRooms(): Observable<Room[]> {
    return this.http.get<Room[]>(this.API_URL);
  }

  createRoom(room: Room): Observable<Room> {
    return this.http.post<Room>(this.API_URL, room);
  }

  deleteRoom(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getRoomByCode(code: string): Observable<Room | undefined> {
    // Podríamos añadir un endpoint específico en el backend, 
    // pero por ahora filtramos del listado o asumimos que el componente lo maneja
    return new Observable(observer => {
      this.getRooms().subscribe(rooms => {
        const room = rooms.find(r => r.codigoSala === code);
        observer.next(room);
        observer.complete();
      });
    });
  }
}
