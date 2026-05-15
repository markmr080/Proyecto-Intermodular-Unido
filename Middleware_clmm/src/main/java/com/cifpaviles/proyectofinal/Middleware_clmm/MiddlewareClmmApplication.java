package com.cifpaviles.proyectofinal.Middleware_clmm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MiddlewareClmmApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiddlewareClmmApplication.class, args);
	}

}
