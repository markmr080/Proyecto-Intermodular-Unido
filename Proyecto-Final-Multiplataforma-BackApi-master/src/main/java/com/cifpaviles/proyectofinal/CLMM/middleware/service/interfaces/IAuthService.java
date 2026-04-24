package com.cifpaviles.proyectofinal.CLMM.middleware.service.interfaces;

import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;

public interface IAuthService {
    /**
     * Orquesta el registro llamando a la API.
     */
    void registrar(RegistroDTO dto);

    /**
     * Orquesta el login: valida con la API y genera el Token JWT con fingerprint.
     * @param dto         credenciales
     * @param fingerprint hash SHA-256 del navegador (puede ser null para tokens internos)
     * @return String con el Token JWT generado.
     */
    String login(LoginDTO dto, String fingerprint);

    /**
     * Valida las credenciales de un usuario final.
     */
    UsuarioEntity validateUser(LoginDTO dto);

    /**
     * Inicia el proceso de recuperación de contraseña.
     */
    void forgotPassword(String email);

    /**
     * Restablece la contraseña de un usuario usando un token de recuperación.
     */
    void resetPassword(String token, String newPassword);

    /**
     * Actualiza directamente la contraseña desde el perfil.
     */
    void updatePassword(String nickname, String newPassword);

    /**
     * Actualiza el nickname de un usuario.
     */
    void updateNickname(String currentNickname, String newNickname);

    /**
     * Actualiza la foto de perfil de un usuario.
     */
    void updateProfilePicture(String nickname, String profilePicture);
}