package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.UserProfileDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.config.security.JwtProvider;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.interfaces.IAuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements IAuthService {

    private final BackendClient backendClient;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    public AuthService(BackendClient backendClient, JwtProvider jwtProvider, EmailService emailService) {
        this.backendClient = backendClient;
        this.jwtProvider = jwtProvider;
        this.emailService = emailService;
    }

    @Override
    public String login(LoginDTO loginDTO, String fingerprint) {
        // --- REGLA DE ORO DEL PDF (RN-01.1) ---
        // Si el username no es "middleware_admin", ni siquiera preguntamos a la base de datos
        if (!"middleware_admin".equals(loginDTO.getUsername())) {
            throw new RuntimeException("401 Unauthorized: Solo el middleware_admin puede autenticarse");
        }

        // 1. Validamos las credenciales contra MySQL (usando el cliente HTTP de la API)
        UserProfileDTO admin = backendClient.validarCredenciales(loginDTO);

        // 2. Si todo es correcto, generamos el token con el fingerprint incrustado
        if (fingerprint != null && !fingerprint.isBlank()) {
            return jwtProvider.generarToken(admin.getUsername(), fingerprint);
        }
        // Fallback sin fingerprint (ej.: herramientas de prueba sin cabecera)
        return jwtProvider.generarToken(admin.getUsername());
    }

    @Override
    public void registrar(RegistroDTO registroDTO) {
        backendClient.registrarUsuario(registroDTO);
        // El Middleware envía directamente el correo de bienvenida
        emailService.enviarCorreoBienvenida(registroDTO.getEmail(), registroDTO.getUsername());
    }

    @Override
    public UserProfileDTO validateUser(LoginDTO dto) {
        // Validar credenciales del usuario final a través del cliente HTTP de API
        return backendClient.validarCredenciales(dto);
    }

    @Override
    public void forgotPassword(String email) {
        // Verifica si el email existe en la API
        backendClient.verificarEmail(email);
        // Token de recuperación: expira en 20 min, subject = email
        String token = jwtProvider.generarTokenRecuperacion(email);
        emailService.enviarCorreoRecuperacion(email, token);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        if (!jwtProvider.validarToken(token)) {
            throw new RuntimeException("TOKEN_INVALIDO");
        }
        String email = jwtProvider.getNombreUsuarioFromToken(token);
        backendClient.actualizarPassword(email, newPassword);
    }

    @Override
    public void updatePassword(String username, String newPassword) {
        backendClient.actualizarPasswordByUsername(username, newPassword);
    }

    @Override
    public void updateNickname(String currentUsername, String newUsername) {
        backendClient.actualizarUsername(currentUsername, newUsername);
    }

    @Override
    public void updateProfilePicture(String username, String profilePicture) {
        backendClient.actualizarProfilePicture(username, profilePicture);
    }
}
