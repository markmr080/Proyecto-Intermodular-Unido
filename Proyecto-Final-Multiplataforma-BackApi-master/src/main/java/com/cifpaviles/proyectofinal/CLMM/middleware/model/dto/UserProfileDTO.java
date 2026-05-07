package com.cifpaviles.proyectofinal.CLMM.middleware.model.dto;

/**
 * DTO para enviar la información pública del perfil de usuario al frontend.
 * No debe contener nunca información sensible como la contraseña.
 */
public class UserProfileDTO {
    private String username;
    private String email;
    private String profilePicture;

    public UserProfileDTO() {
    }

    public UserProfileDTO(String username, String email, String profilePicture) {
        this.username = username;
        this.email = email;
        this.profilePicture = profilePicture;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
