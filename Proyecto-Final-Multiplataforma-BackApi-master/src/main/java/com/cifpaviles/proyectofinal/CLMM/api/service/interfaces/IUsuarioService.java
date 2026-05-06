package com.cifpaviles.proyectofinal.CLMM.api.service.interfaces;

import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;

public interface IUsuarioService {

    /**
     * Valida si el usuario existe y si la contraseña coincide.
     * @return El objeto UsuarioEntity si es válido.
     * @throws RuntimeException si las credenciales son incorrectas.
     */
    UsuarioEntity validarCredenciales(LoginDTO datos);

    /**
     * Crea un nuevo usuario en la base de datos y dispara el correo de bienvenida.
     */
    void registrarUsuario(RegistroDTO datos);

    /**
     * Verifica que un correo existe en la BD. Lanza excepción si no.
     */
    void verificarEmail(String email);

    /**
     * Usa el servicio de email para enviar el token.
     */
    void enviarCorreoRecuperacion(String email, String token);

    /**
     * Actualiza la contraseña del usuario por email (flujo reset-password).
     */
    void actualizarPassword(String email, String newPassword);

    /**
     * Actualiza la contraseña del usuario por username.
     */
    void actualizarPasswordByUsername(String username, String newPassword);

    /**
     * Actualiza el username de un usuario.
     * @throws RuntimeException si el nuevo username ya está en uso.
     */
    void actualizarUsername(String currentUsername, String newUsername);

    /**
     * Actualiza la foto de perfil de un usuario.
     */
    void actualizarProfilePicture(String username, String profilePicture);
}