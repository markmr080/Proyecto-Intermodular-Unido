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

    // Ejemplo de identificación explícita: Buscar por ID como parámetro
    @GetMapping("/{usuarioId}")
    public ResponseEntity<?> obtenerJugador(@PathVariable Long usuarioId) {
        // Aquí llamarías a un método del service que busque por ID
        // return ResponseEntity.ok(usuarioService.findById(usuarioId));
        return ResponseEntity.ok(Map.of("id", usuarioId, "info", "Datos del jugador"));
    }
}
