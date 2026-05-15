package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.config.socket;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cifpaviles.proyectofinal.Middleware_clmm.middleware.config.security.JwtProvider;

@Configuration
public class SocketIOConfig {

    @Value("${socketio.host:0.0.0.0}")
    private String host;

    @Value("${socketio.port:8081}")
    private Integer port;

    private final JwtProvider jwtProvider;

    public SocketIOConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        // Permitir CORS desde Angular
        config.setOrigin("*");

        // Configurar la validación JWT en el handshake
        config.setAuthorizationListener(data -> {
            String token = data.getSingleUrlParam("token");
            if (token != null && jwtProvider.validarToken(token)) {
                return true;
            }
            System.err.println("Conexión Socket.IO rechazada: Token inválido o ausente.");
            return false;
        });

        return new SocketIOServer(config);
    }
}
