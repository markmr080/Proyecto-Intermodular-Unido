import { Injectable } from '@angular/core';

export interface Room {
  id: string;
  name: string;
  code: string;
  ownerPic: string;
  ownerName?: string;
  currentPlayers: number;
  maxPlayers: number;
  createdAt: number; // Timestamp
}

@Injectable({
  providedIn: 'root'
})
export class RoomService {
  private readonly ROOMS_KEY = 'mock_rooms_db';
  private rooms: Room[] = [];
  private readonly MAX_ROOM_LIFE_MS = 15 * 60 * 1000; // 15 minutos

  constructor() {
    this.loadRooms();
    this.cleanExpiredRooms();

    // Si no hay salas, creamos las salas iniciales de ejemplo
    if (this.rooms.length === 0) {
      this.initDefaultRooms();
    }
  }

  private loadRooms() {
    const saved = localStorage.getItem(this.ROOMS_KEY);
    if (saved) {
      this.rooms = JSON.parse(saved);
    }
  }

  private saveRooms() {
    localStorage.setItem(this.ROOMS_KEY, JSON.stringify(this.rooms));
  }

  getRooms(): Room[] {
    this.cleanExpiredRooms();
    return this.rooms;
  }

  getRoomByCode(code: string): Room | undefined {
    this.cleanExpiredRooms();
    return this.rooms.find(r => r.code === code);
  }

  createRoom(name: string, ownerPic: string, ownerName: string): Room {
    const newRoom: Room = {
      id: Math.random().toString(36).substring(2, 9),
      name: name,
      code: Math.random().toString(36).substring(2, 8).toUpperCase(),
      ownerPic: ownerPic,
      ownerName: ownerName,
      currentPlayers: 1,
      maxPlayers: 2,
      createdAt: Date.now()
    };
    this.rooms.push(newRoom);
    this.saveRooms();
    return newRoom;
  }

  deleteRoom(code: string) {
    this.rooms = this.rooms.filter(r => r.code !== code);
    this.saveRooms();
  }

  updateRoomOwner(code: string, newOwnerName: string) {
    const room = this.rooms.find(r => r.code === code);
    if (room) {
      room.ownerName = newOwnerName;
      room.name = `Partida de ${newOwnerName}`;
      this.saveRooms();
    }
  }

  cleanExpiredRooms() {
    const now = Date.now();
    const initialCount = this.rooms.length;
    this.rooms = this.rooms.filter(room => (now - room.createdAt) < this.MAX_ROOM_LIFE_MS);

    if (this.rooms.length !== initialCount) {
      this.saveRooms();
    }
  }

  getTimeLeftSeconds(room: Room): number {
    const now = Date.now();
    const elapsed = now - room.createdAt;
    const remaining = this.MAX_ROOM_LIFE_MS - elapsed;
    return Math.max(0, Math.floor(remaining / 1000));
  }

  private initDefaultRooms() {
    this.rooms = [
      {
        id: '1',
        name: 'Partida de Thor',
        code: 'A1B2',
        ownerPic: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Thor',
        ownerName: 'Thor',
        currentPlayers: 1,
        maxPlayers: 2,
        createdAt: Date.now()
      },
      {
        id: '2',
        name: 'Sala Vacia',
        code: 'X9Y8',
        ownerPic: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Loki',
        ownerName: 'Loki',
        currentPlayers: 2,
        maxPlayers: 2,
        createdAt: Date.now() - (5 * 60 * 1000) // Creada hace 5 mins para probar
      },
      {
        id: '3',
        name: 'Odins hall',
        code: 'O3D4',
        ownerPic: 'https://api.dicebear.com/7.x/adventurer/svg?seed=Odin',
        ownerName: 'Odin',
        currentPlayers: 1,
        maxPlayers: 2,
        createdAt: Date.now() - (10 * 60 * 1000) // Creada hace 10 mins para probar
      }
    ];
    this.saveRooms();
  }
}
