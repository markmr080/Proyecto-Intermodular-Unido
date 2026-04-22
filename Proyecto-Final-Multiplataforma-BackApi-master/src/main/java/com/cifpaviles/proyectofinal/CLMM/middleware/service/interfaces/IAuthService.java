package com.cifpaviles.proyectofinal.CLMM.middleware.service.interfaces;

import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.RegistroDTO;

public interface IAuthService {
    /**
     * Orquesta el registro llamando a la API.
     */
    void registrar(RegistroDTO dto);

    /**
     * Orquesta el login: valida con la API y genera el Token JWT.
     * @return String con el Token JWT generado.
     */
    String login(LoginDTO dto);

    /**
     * Valida las credenciales de un usuario final.
     */
    void validateUser(LoginDTO dto);

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
}