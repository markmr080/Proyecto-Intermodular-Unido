package com.cifpaviles.proyectofinal.CLMM.middleware.model.dto;

public class LoginDTO {

    private String nickname;
    private String password;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setContrasenia(String contrasenia) {
        this.password = contrasenia;
    }
}
