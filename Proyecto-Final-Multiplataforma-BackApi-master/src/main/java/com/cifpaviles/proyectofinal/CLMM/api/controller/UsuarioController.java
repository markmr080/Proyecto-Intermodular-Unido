package com.cifpaviles.proyectofinal.CLMM.api.controller;

import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IUsuarioService;
import com.cifpaviles.proyectofinal.CLMM.api.model.dto.RegistroDTO;
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

    @PostMapping("/validar")
    public ResponseEntity<?> validarCredenciales(@Valid @RequestBody com.cifpaviles.proyectofinal.CLMM.api.model.dto.LoginDTO loginDTO) {
        try {
            return ResponseEntity.ok(usuarioService.validarCredenciales(loginDTO));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if ("USUARIO_NO_ENCONTRADO".equals(msg) || "PASSWORD_INCORRECTO".equals(msg)) {
                return ResponseEntity.status(401).body(Map.of("error", msg));
            }
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_INTERNO"));
        }
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> crearJugador(@Valid @RequestBody RegistroDTO registroDTO) {
        try {
            usuarioService.registrarUsuario(registroDTO);
            return ResponseEntity.ok(Map.of("message", "Jugador registrado con éxito"));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if ("EMAIL_DUPLICADO".equals(msg) || "USERNAME_DUPLICADO".equals(msg))
                return ResponseEntity.status(409).body(Map.of("error", msg));
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_INTERNO"));
        }
    }

    @GetMapping("/verificar-email")
    public ResponseEntity<?> verificarEmail(@RequestParam String email) {
        usuarioService.verificarEmail(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/correo-recuperacion")
    public ResponseEntity<?> enviarCorreoRecuperacion(@RequestParam String email, @RequestParam String token) {
        usuarioService.enviarCorreoRecuperacion(email, token);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    public ResponseEntity<?> actualizarPassword(@RequestBody com.cifpaviles.proyectofinal.CLMM.api.model.dto.LoginDTO dto) {
        usuarioService.actualizarPassword(dto.getUsername(), dto.getPassword());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{username}/password")
    public ResponseEntity<?> actualizarPasswordByUsername(@PathVariable String username, @RequestBody String newPassword) {
        usuarioService.actualizarPasswordByUsername(username, newPassword);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{username}/nickname")
    public ResponseEntity<?> actualizarUsername(@PathVariable String username, @RequestBody String newUsername) {
        usuarioService.actualizarUsername(username, newUsername);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{username}/profile-picture")
    public ResponseEntity<?> actualizarProfilePicture(@PathVariable String username, @RequestBody String profilePicture) {
        usuarioService.actualizarProfilePicture(username, profilePicture);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/profile")
    public ResponseEntity<?> obtenerJugador(@PathVariable String username) {
        try {
            com.cifpaviles.proyectofinal.CLMM.api.model.dto.UserProfileDTO profile = usuarioService.getProfileByUsername(username);
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
