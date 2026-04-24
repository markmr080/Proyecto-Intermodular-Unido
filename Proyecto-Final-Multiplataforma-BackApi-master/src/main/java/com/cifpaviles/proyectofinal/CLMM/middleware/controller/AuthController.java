package com.cifpaviles.proyectofinal.CLMM.middleware.controller;

import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.UpdateProfilePictureDTO;
import com.cifpaviles.proyectofinal.CLMM.middleware.service.interfaces.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200") // ¡No olvides esto para Angular!
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    // RF-01: Endpoint de Login de administrador
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        // Recogemos el fingerprint del navegador (si lo envía el frontend)
        String fingerprint = request.getHeader("X-Fingerprint");
        String token = authService.login(loginDTO, fingerprint);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "message", "Middleware authenticated successfully"
        ));
    }

    // OJO: Según el PDF (punto 2.2), este endpoint debería requerir Token de Admin
    // porque "Todos los endpoints requieren token (excepto /api/auth/login)"
    @PostMapping("/register")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroDTO registroDTO) {
        authService.registrar(registroDTO);
        return ResponseEntity.ok(Map.of(
                "message", "Usuario " + registroDTO.getNickname() + " registrado con éxito por el Middleware"
        ));
    }

    // Endpoint para que el middleware valide las credenciales de un usuario final
    @PostMapping("/validate-user")
    public ResponseEntity<?> validateUser(@Valid @RequestBody LoginDTO loginDTO) {
        com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity user = authService.validateUser(loginDTO);
        return ResponseEntity.ok(Map.of(
                "message", "User credentials valid",
                "nickname", user.getNickname(),
                "profilePicture", user.getProfilePicture() != null ? user.getProfilePicture() : ""
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.ForgotPasswordDTO dto) {
        authService.forgotPassword(dto.getEmail());
        return ResponseEntity.ok(Map.of("message", "Correo de recuperación enviado (si el email existe)"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.ResetPasswordDTO dto) {
        authService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada con éxito"));
    }

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(@Valid @RequestBody com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.UpdatePasswordDTO dto) {
        authService.updatePassword(dto.getNickname(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    @PostMapping("/update-nickname")
    public ResponseEntity<?> updateNickname(@Valid @RequestBody com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.UpdateNicknameDTO dto) {
        authService.updateNickname(dto.getCurrentNickname(), dto.getNewNickname());
        return ResponseEntity.ok(Map.of("message", "Nickname actualizado correctamente"));
    }

    @PostMapping("/update-profile-picture")
    public ResponseEntity<?> updateProfilePicture(@Valid @RequestBody UpdateProfilePictureDTO dto) {
        authService.updateProfilePicture(dto.getNickname(), dto.getProfilePicture());
        return ResponseEntity.ok(Map.of("message", "Foto de perfil actualizada correctamente"));
    }
}