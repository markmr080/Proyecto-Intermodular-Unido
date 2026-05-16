package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.controller;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.PersonajeDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Proxy del catálogo de personajes hacia el Backend API.
 * El frontend SIEMPRE llama a este endpoint (Middleware:8080/api/personajes),
 * nunca directamente al Backend API.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/personajes")
public class PersonajesController {

    private final BackendClient backendClient;

    public PersonajesController(BackendClient backendClient) {
        this.backendClient = backendClient;
    }

    @GetMapping
    public ResponseEntity<List<PersonajeDTO>> getPersonajes() {
        List<PersonajeDTO> personajes = backendClient.getPersonajes();
        return ResponseEntity.ok(personajes);
    }
}
