package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.controller;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.ForgotPasswordDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.ResetPasswordDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.UpdateNicknameDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.UpdatePasswordDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.UpdateProfilePictureDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.UserProfileDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.interfaces.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        try {
            authService.registrar(registroDTO);
            return ResponseEntity.ok(Map.of(
                    "message", "Usuario " + registroDTO.getUsername() + " registrado con éxito por el Middleware"
            ));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("EMAIL_DUPLICADO"))
                return ResponseEntity.status(409).body(Map.of("error", "EMAIL_DUPLICADO", "message", "El correo ya está en uso"));
            if (msg.contains("USERNAME_DUPLICADO"))
                return ResponseEntity.status(409).body(Map.of("error", "USERNAME_DUPLICADO", "message", "El nombre de usuario ya está en uso"));
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_REGISTRO", "message", "Error al registrar el usuario"));
        }
    }

    // Endpoint para que el middleware valide las credenciales de un usuario final
    @PostMapping("/validate-user")
    public ResponseEntity<?> validateUser(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            UserProfileDTO user = authService.validateUser(loginDTO);
            return ResponseEntity.ok(Map.of(
                    "message", "User credentials valid",
                    "username", user.getUsername(),
                    "profilePicture", user.getProfilePicture() != null ? user.getProfilePicture() : ""
            ));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if ("USUARIO_NO_ENCONTRADO".equals(msg) || "PASSWORD_INCORRECTO".equals(msg)) {
                // The frontend checks if (err.error === 'USUARIO_NO_ENCONTRADO') so we return the raw string or format it as the frontend expects it
                // Wait! In the frontend: "if (err.error === 'USUARIO_NO_ENCONTRADO')"
                // If we return ResponseEntity.status(401).body(msg), Spring boot converts it to plain text "USUARIO_NO_ENCONTRADO"
                return ResponseEntity.status(401).body(msg);
            }
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_INTERNO"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        authService.forgotPassword(dto.getEmail());
        return ResponseEntity.ok(Map.of("message", "Correo de recuperación enviado (si el email existe)"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada con éxito"));
    }

    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        authService.updatePassword(dto.getUsername(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    @PostMapping("/update-nickname")
    public ResponseEntity<?> updateNickname(@Valid @RequestBody UpdateNicknameDTO dto) {
        authService.updateNickname(dto.getCurrentUsername(), dto.getNewUsername());
        return ResponseEntity.ok(Map.of("message", "Username actualizado correctamente"));
    }

    @PostMapping("/update-profile-picture")
    public ResponseEntity<?> updateProfilePicture(@Valid @RequestBody UpdateProfilePictureDTO dto) {
        authService.updateProfilePicture(dto.getUsername(), dto.getProfilePicture());
        return ResponseEntity.ok(Map.of("message", "Foto de perfil actualizada correctamente"));
    }
}
