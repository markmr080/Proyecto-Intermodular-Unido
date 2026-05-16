package com.cifpaviles.proyectofinal.CLMM.api.service.impl;

import com.cifpaviles.proyectofinal.CLMM.api.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.CLMM.api.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEstadisticasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private UsuarioService usuarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IEstadisticasService estadisticasService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder, estadisticasService);
    }

    @Test
    void testRegistrarUsuario_Exito() {
        RegistroDTO dto = new RegistroDTO();
        dto.setUsername("testuser");
        dto.setEmail("test@email.com");
        dto.setPassword("password123");
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        usuarioService.registrarUsuario(dto);

        verify(usuarioRepository, times(1)).save(any(UsuarioEntity.class));
    }

    @Test
    void testRegistrarUsuario_EmailDuplicado() {
        RegistroDTO dto = new RegistroDTO();
        dto.setUsername("testuser");
        dto.setEmail("test@email.com");
        dto.setPassword("password123");
        when(usuarioRepository.existsByEmail("test@email.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.registrarUsuario(dto);
        });

        assertEquals("EMAIL_DUPLICADO", exception.getMessage());
        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    void testValidarCredenciales_Exito() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("password123");
        UsuarioEntity user = new UsuarioEntity("testuser", "test@email.com", "encodedPassword");
        
        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        UsuarioEntity result = usuarioService.validarCredenciales(dto);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testValidarCredenciales_PasswordIncorrecto() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("testuser");
        dto.setPassword("wrongpassword");
        UsuarioEntity user = new UsuarioEntity("testuser", "test@email.com", "encodedPassword");
        
        when(usuarioRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            usuarioService.validarCredenciales(dto);
        });

        assertEquals("PASSWORD_INCORRECTO", exception.getMessage());
    }
}
