package com.cifpaviles.proyectofinal.CLMM.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Le decimos a Spring que use BCrypt para encriptar las contraseñas
        return new BCryptPasswordEncoder();
    }
}