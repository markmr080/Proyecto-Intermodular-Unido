package com.cifpaviles.proyectofinal.CLMM;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ClmmApplication {

    public static void main(String[] args) {
        // Esta línea es la que "enciende" Spring y conecta con la base de datos
        SpringApplication.run(ClmmApplication.class, args);
    }

}