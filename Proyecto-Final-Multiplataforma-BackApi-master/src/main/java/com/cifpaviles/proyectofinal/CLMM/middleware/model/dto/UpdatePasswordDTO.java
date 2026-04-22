package com.cifpaviles.proyectofinal.CLMM.middleware.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdatePasswordDTO {

    @NotBlank(message = "El nickname no puede estar vacío")
    private String nickname;

    @NotBlank(message = "La nueva contraseña no puede estar vacía")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String newPassword;

    public UpdatePasswordDTO() {
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
