package com.cifpaviles.proyectofinal.CLMM.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para actualizar la foto de perfil de un usuario.
 * Campo renombrado: nickname → username.
 */
public class UpdateProfilePictureDTO {

    @NotBlank(message = "El username es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "El username solo puede contener letras, números y guiones bajos")
    private String username;

    @NotBlank(message = "La URL de la imagen es obligatoria")
    private String profilePicture;

    public UpdateProfilePictureDTO() {}

    public UpdateProfilePictureDTO(String username, String profilePicture) {
        this.username = username;
        this.profilePicture = profilePicture;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
