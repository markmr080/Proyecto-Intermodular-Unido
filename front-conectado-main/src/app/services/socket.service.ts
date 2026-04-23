import { Injectable } from '@angular/core';
import io from 'socket.io-client';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SocketService {
    private socket: SocketIOClient.Socket;

    // Subjects para notificar a los componentes
    public nuevaSolicitud$ = new Subject<any>();
    public solicitudAceptada$ = new Subject<string>();
    public solicitudRechazada$ = new Subject<string>();
    public jugadorUnido$ = new Subject<any>();
    public salaCerrada$ = new Subject<string>();

    constructor() {
        // Apuntamos al puerto 8085 del backend
        this.socket = io('http://localhost:8085');

        // Escuchar eventos globales
        this.socket.on('nueva-solicitud', (data: any) => this.nuevaSolicitud$.next(data));
        this.socket.on('solicitud-aceptada', (codigo: string) => this.solicitudAceptada$.next(codigo));
        this.socket.on('solicitud-rechazada', (msg: string) => this.solicitudRechazada$.next(msg));
        this.socket.on('jugador-unido', (data: any) => this.jugadorUnido$.next(data));
        this.socket.on('sala-cerrada', (msg: string) => this.salaCerrada$.next(msg));

        this.socket.on('respuesta-servidor', (data: any) => {
            console.log('Mensaje del servidor:', data);
        });
    }

    registrarUsuario(userId: string) {
        this.socket.emit('registrar-usuario', userId);
    }

    solicitarUnirse(codigoSala: string, user: any) {
        this.socket.emit('solicitar-unirse', {
            codigoSala,
            requesterId: user.username,
            requesterName: user.username,
            requesterAvatar: user.profilePicture
        });
    }

    aceptarSolicitud(codigoSala: string, requester: any) {
        this.socket.emit('aceptar-solicitud', {
            codigoSala,
            requesterId: requester.requesterId,
            requesterName: requester.requesterName,
            requesterAvatar: requester.requesterAvatar
        });
    }

    rechazarSolicitud(requesterId: string) {
        this.socket.emit('rechazar-solicitud', requesterId);
    }

    cerrarSala(codigoSala: string) {
        this.socket.emit('cerrar-sala', codigoSala);
    }

    enviarPrueba(mensaje: string) {
        this.socket.emit('prueba-cliente', mensaje);
    }
}
