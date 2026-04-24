package com.cifpaviles.proyectofinal.CLMM.middleware.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateNicknameDTO {

    @NotBlank(message = "El nickname actual no puede estar vacío")
    private String currentNickname;

    @NotBlank(message = "El nuevo nickname no puede estar vacío")
    @Size(min = 3, max = 30, message = "El nickname debe tener entre 3 y 30 caracteres")
    private String newNickname;

    public UpdateNicknameDTO() {
    }

    public String getCurrentNickname() {
        return currentNickname;
    }

    public void setCurrentNickname(String currentNickname) {
        this.currentNickname = currentNickname;
    }

    public String getNewNickname() {
        return newNickname;
    }

    public void setNewNickname(String newNickname) {
        this.newNickname = newNickname;
    }
}
