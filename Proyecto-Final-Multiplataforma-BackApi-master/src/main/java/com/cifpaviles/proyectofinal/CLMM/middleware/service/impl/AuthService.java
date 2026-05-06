package com.cifpaviles.proyectofinal.CLMM.middleware.service.impl;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IUsuarioService;
import com.cifpaviles.proyectofinal.CLMM.middleware.config.security.JwtProvider;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.CLMM.middleware.service.interfaces.IAuthService;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements IAuthService {

    private final IUsuarioService usuarioService; // El servicio de la API (MySQL)
    private final JwtProvider jwtProvider;

    public AuthService(IUsuarioService usuarioService, JwtProvider jwtProvider) {
        this.usuarioService = usuarioService;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public String login(LoginDTO loginDTO, String fingerprint) {
        // --- REGLA DE ORO DEL PDF (RN-01.1) ---
        // Si el username no es "middleware_admin", ni siquiera preguntamos a la base de datos
        if (!"middleware_admin".equals(loginDTO.getUsername())) {
            throw new RuntimeException("401 Unauthorized: Solo el middleware_admin puede autenticarse");
        }

        // 1. Validamos las credenciales contra MySQL (usando el servicio de la API)
        UsuarioEntity admin = usuarioService.validarCredenciales(loginDTO);

        // 2. Si todo es correcto, generamos el token con el fingerprint incrustado
        if (fingerprint != null && !fingerprint.isBlank()) {
            return jwtProvider.generarToken(admin.getUsername(), fingerprint);
        }
        // Fallback sin fingerprint (ej.: herramientas de prueba sin cabecera)
        return jwtProvider.generarToken(admin.getUsername());
    }

    @Override
    public void registrar(RegistroDTO registroDTO) {
        // El registro de usuarios (jugadores) ahora pasa por aquí.
        // Como el Middleware es el que manda, él usa el servicio de la API para guardar.
        usuarioService.registrarUsuario(registroDTO);
    }

    @Override
    public UsuarioEntity validateUser(LoginDTO dto) {
        // Validar credenciales del usuario final a través del servicio de API
        return usuarioService.validarCredenciales(dto);
    }

    @Override
    public void forgotPassword(String email) {
        // Verifica si el email existe
        usuarioService.verificarEmail(email);
        // Genera token usando el email como payload
        String token = jwtProvider.generarToken(email);
        usuarioService.enviarCorreoRecuperacion(email, token);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        if (!jwtProvider.validarToken(token)) {
            throw new RuntimeException("TOKEN_INVALIDO");
        }
        String email = jwtProvider.getNombreUsuarioFromToken(token);
        usuarioService.actualizarPassword(email, newPassword);
    }

    @Override
    public void updatePassword(String username, String newPassword) {
        usuarioService.actualizarPasswordByUsername(username, newPassword);
    }

    @Override
    public void updateNickname(String currentUsername, String newUsername) {
        usuarioService.actualizarUsername(currentUsername, newUsername);
    }

    @Override
    public void updateProfilePicture(String username, String profilePicture) {
        usuarioService.actualizarProfilePicture(username, profilePicture);
    }
}