package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.service.impl;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.LoginDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.RegistroDTO;
import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.UserProfileDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BackendClient {

    private final RestClient restClient;

    public BackendClient(
            @Value("${backend.api.url:http://localhost:8081}") String backendUrl,
            @Value("${internal.api.key}") String internalApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(backendUrl)
                .defaultHeader("X-Internal-Key", internalApiKey) // ← handshake con la API
                .build();
    }

    public UserProfileDTO validarCredenciales(LoginDTO loginDTO) {
        return restClient.post()
                .uri("/api/user/validar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginDTO)
                .retrieve()
                .body(UserProfileDTO.class);
    }

    public void registrarUsuario(RegistroDTO registroDTO) {
        restClient.post()
                .uri("/api/user/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registroDTO)
                .retrieve()
                .toBodilessEntity();
    }

    public void verificarEmail(String email) {
        restClient.get()
                .uri("/api/user/verificar-email?email=" + email)
                .retrieve()
                .toBodilessEntity();
    }

    public void enviarCorreoRecuperacion(String email, String token) {
        restClient.post()
                .uri("/api/user/correo-recuperacion?email=" + email + "&token=" + token)
                .retrieve()
                .toBodilessEntity();
    }

    public void actualizarPassword(String email, String newPassword) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(email); // Depende de cómo esté tu LoginDTO, usamos username como email
        dto.setPassword(newPassword);

        restClient.put()
                .uri("/api/user/password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

    public void actualizarPasswordByUsername(String username, String newPassword) {
        restClient.put()
                .uri("/api/user/" + username + "/password")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newPassword)
                .retrieve()
                .toBodilessEntity();
    }

    public void actualizarUsername(String currentUsername, String newUsername) {
        restClient.put()
                .uri("/api/user/" + currentUsername + "/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .body(newUsername)
                .retrieve()
                .toBodilessEntity();
    }

    public void actualizarProfilePicture(String username, String profilePicture) {
        restClient.put()
                .uri("/api/user/" + username + "/profile-picture")
                .contentType(MediaType.APPLICATION_JSON)
                .body(profilePicture)
                .retrieve()
                .toBodilessEntity();
    }

    public UserProfileDTO getProfileByUsername(String username) {
        return restClient.get()
                .uri("/api/user/" + username + "/profile")
                .retrieve()
                .body(UserProfileDTO.class);
    }

    public Long crearPartida(String hostUsername) {
        return restClient.post()
                .uri("/api/partidas/crear?host=" + hostUsername)
                .retrieve()
                .body(Long.class);
    }

    public void actualizarEstadoPartida(Long idPartida, String estado) {
        restClient.put()
                .uri("/api/partidas/" + idPartida + "/estado?estado=" + estado)
                .retrieve()
                .toBodilessEntity();
    }

    public com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.StatsAgregadasDTO getEstadisticasJugador(String username) {
        return restClient.get()
                .uri("/api/estadisticas/jugador/" + username)
                .retrieve()
                .body(com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.StatsAgregadasDTO.class);
    }

    public java.util.List<com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.PersonajeDTO> getPersonajes() {
        return restClient.get()
                .uri("/api/personajes")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<java.util.List<com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto.PersonajeDTO>>() {});
    }

    public void guardarStats(String username, String personajeNombre,
                             int hitsAcertados, int hitsFallados, int barcosHundidos, boolean ganador) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("username",       username);
        body.put("personajeNombre",personajeNombre);
        body.put("hitsAcertados",  hitsAcertados);
        body.put("hitsFallados",   hitsFallados);
        body.put("barcosHundidos", barcosHundidos);
        body.put("ganador",        ganador);
        restClient.post()
                .uri("/api/estadisticas/guardar")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
