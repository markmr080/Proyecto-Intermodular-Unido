package com.cifpaviles.proyectofinal.CLMM.middleware.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para actualizar el username de un usuario.
 * Los campos hacen referencia a 'username' en lugar de 'nickname'.
 */
public class UpdateNicknameDTO {

    @NotBlank(message = "El username actual no puede estar vacío")
    private String currentUsername;

    @NotBlank(message = "El nuevo username no puede estar vacío")
    @Size(min = 3, max = 100, message = "El username debe tener entre 3 y 100 caracteres")
    private String newUsername;

    public UpdateNicknameDTO() {}

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }
}
