import { Injectable } from '@angular/core';
import io from 'socket.io-client';

@Injectable({ providedIn: 'root' })
export class SocketService {
    private socket: SocketIOClient.Socket;

    constructor() {
        // Apuntamos al puerto 8085 del backend
        this.socket = io('http://localhost:8085');

        // Escuchamos la respuesta del servidor
        this.socket.on('respuesta-servidor', (data: any) => {
            console.log('Mensaje del servidor:', data);
        });
    }

    enviarPrueba(mensaje: string) {
        // Emitimos el evento que el servidor está esperando
        this.socket.emit('prueba-cliente', mensaje);
    }
}