package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.controller;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl.BackendClient;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.RegistroDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UsuarioController {

    private final BackendClient backendClient;


    public UsuarioController(BackendClient backendClient) {
        this.backendClient = backendClient;

    }

    @PostMapping
    public ResponseEntity<?> crearJugador(@Valid @RequestBody RegistroDTO registroDTO) {
        backendClient.registrarUsuario(registroDTO);
        return ResponseEntity.ok(Map.of("message", "Jugador registrado con éxito"));
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> obtenerJugador(@PathVariable String username) {
        try {
            com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.UserProfileDTO profile = backendClient.getProfileByUsername(username);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
