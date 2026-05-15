package com.cifpaviles.proyectofinal.CLMM.api.service.impl;

import com.cifpaviles.proyectofinal.CLMM.api.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.CLMM.api.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IUsuarioService;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEstadisticasService;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEstadisticasService estadisticasService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, IEstadisticasService estadisticasService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.estadisticasService = estadisticasService;
    }

    @PostConstruct
    public void crearUsuarioSistema() {
        // RF-01: El username debe ser exactamente "middleware_admin"
        if (!usuarioRepository.existsByUsername("middleware_admin")) {
            UsuarioEntity admin = new UsuarioEntity();
            admin.setUsername("middleware_admin");
            admin.setEmail("admin@sistema.internal");
            // Esta clave es la que usará el furgón (Angular) para identificarse
            admin.setPasswordHash(passwordEncoder.encode("clave_secreta_del_middleware_2026"));
            admin.setRole("ADMIN");

            usuarioRepository.save(admin);
            System.out.println("✅ Entidad 'middleware_admin' creada en MySQL (Persistencia transaccional).");
        }
    }

    @Override
    public void registrarUsuario(RegistroDTO datos) {
        if (usuarioRepository.existsByEmail(datos.getEmail()))
            throw new RuntimeException("EMAIL_DUPLICADO");

        if (usuarioRepository.existsByUsername(datos.getUsername()))
            throw new RuntimeException("USERNAME_DUPLICADO");

        UsuarioEntity user = new UsuarioEntity(
                datos.getUsername(),
                datos.getEmail(),
                passwordEncoder.encode(datos.getPassword()));

        user.setRole("USER");

        usuarioRepository.save(user);
    }

    @Override
    public UsuarioEntity validarCredenciales(LoginDTO datos) {
        // Buscamos por username (RF-01)
        UsuarioEntity user = usuarioRepository.findByUsername(datos.getUsername())
                .orElseThrow(() -> new RuntimeException("USUARIO_NO_ENCONTRADO"));

        if (!passwordEncoder.matches(datos.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("PASSWORD_INCORRECTO");
        }
        return user;
    }

    @Override
    public void verificarEmail(String email) {
        if (!usuarioRepository.existsByEmail(email)) {
            throw new RuntimeException("EMAIL_NO_ENCONTRADO");
        }
    }

    @Override
    public void enviarCorreoRecuperacion(String email, String token) {
        // Delegado al Middleware
        System.out.println("API Backend: Petición de recuperación recibida. El envío de correo es gestionado por el Middleware.");
    }

    @Override
    public void actualizarPassword(String email, String newPassword) {
        UsuarioEntity user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("USUARIO_NO_ENCONTRADO"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        usuarioRepository.save(user);
    }

    @Override
    public void actualizarPasswordByUsername(String username, String newPassword) {
        UsuarioEntity user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("USUARIO_NO_ENCONTRADO"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        usuarioRepository.save(user);
    }

    @Override
    public void actualizarUsername(String currentUsername, String newUsername) {
        if (usuarioRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("USERNAME_DUPLICADO");
        }

        UsuarioEntity user = usuarioRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("USUARIO_NO_ENCONTRADO"));
        user.setUsername(newUsername);
        usuarioRepository.save(user);

        // Actualizamos también las estadísticas en MongoDB
        estadisticasService.actualizarUsernameStats(user.getId(), newUsername);
    }

    @Override
    public void actualizarProfilePicture(String username, String profilePicture) {
        UsuarioEntity user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("USUARIO_NO_ENCONTRADO"));
        user.setProfilePicture(profilePicture);
        usuarioRepository.save(user);
    }

    @Override
    public com.cifpaviles.proyectofinal.CLMM.api.model.dto.UserProfileDTO getProfileByUsername(String username) {
        UsuarioEntity user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("USUARIO_NO_ENCONTRADO"));
        return new com.cifpaviles.proyectofinal.CLMM.api.model.dto.UserProfileDTO(
                user.getUsername(),
                user.getEmail(),
                user.getProfilePicture()
        );
    }
}
