package com.cifpaviles.proyectofinal.CLMM.middleware.model.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateProfilePictureDTO {

    @NotBlank(message = "El nickname es obligatorio")
    private String nickname;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    private String profilePicture;

    public UpdateProfilePictureDTO() {
    }

    public UpdateProfilePictureDTO(String nickname, String profilePicture) {
        this.nickname = nickname;
        this.profilePicture = profilePicture;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
