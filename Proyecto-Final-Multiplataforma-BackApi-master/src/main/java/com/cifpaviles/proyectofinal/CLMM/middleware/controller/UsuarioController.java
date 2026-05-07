package com.cifpaviles.proyectofinal.CLMM.middleware.controller;

import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IUsuarioService;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.RegistroDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UsuarioController {

    private final IUsuarioService usuarioService;


    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;

    }

    @PostMapping
    public ResponseEntity<?> crearJugador(@Valid @RequestBody RegistroDTO registroDTO) {
        usuarioService.registrarUsuario(registroDTO);
        return ResponseEntity.ok(Map.of("message", "Jugador registrado con éxito"));
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> obtenerJugador(@PathVariable String username) {
        try {
            com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.UserProfileDTO profile = usuarioService.getProfileByUsername(username);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
