package com.cifpaviles.proyectofinal.CLMM.api.service.interfaces;

public interface IEmailService {
    /**
     * Envía un correo de forma asíncrona tras un registro exitoso.
     */
    void enviarCorreoBienvenida(String destinatario, String nombreUsuario);
    
    /**
     * Envía un correo con el enlace de recuperación de contraseña.
     */
    void enviarCorreoRecuperacion(String destinatario, String token);
}