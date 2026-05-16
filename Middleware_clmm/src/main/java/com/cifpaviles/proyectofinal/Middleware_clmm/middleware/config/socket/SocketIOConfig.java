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
        config.setOrigin("*");

        // Optimización de hilos para evitar bloqueos
        config.setWorkerThreads(100);
        config.setPingTimeout(60000);
        config.setPingInterval(25000);

        // Manejador de excepciones compatible con versión 1.7.19
        config.setExceptionListener(new com.corundumstudio.socketio.listener.DefaultExceptionListener() {
            @Override
            public boolean exceptionCaught(io.netty.channel.ChannelHandlerContext ctx, Throwable e) throws Exception {
                if (e.getMessage() != null && (e.getMessage().contains("Connection reset") || e instanceof java.io.IOException)) {
                    return true; // Consumir el error
                }
                return super.exceptionCaught(ctx, e);
            }

            @Override
            public void onConnectException(Exception e, com.corundumstudio.socketio.SocketIOClient client) {
                if (e.getMessage() != null && e.getMessage().contains("Connection reset")) return;
                super.onConnectException(e, client);
            }
        });

        config.setAuthorizationListener(data -> {
            String token = data.getSingleUrlParam("token");
            return token != null && jwtProvider.validarToken(token);
        });

        return new SocketIOServer(config);
    }
}
