package com.cifpaviles.proyectofinal.CLMM.api.service.impl;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaStatsDocument;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mongo.EstadisticasRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEstadisticasService;
import com.cifpaviles.proyectofinal.CLMM.api.model.dto.StatsAgregadasDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadisticasService implements IEstadisticasService {

    private final EstadisticasRepository mongoStatsRepository;
    private final UsuarioRepository usuarioRepository;

    public EstadisticasService(EstadisticasRepository mongoStatsRepository, 
                               UsuarioRepository usuarioRepository) {
        this.mongoStatsRepository = mongoStatsRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public StatsAgregadasDTO getStatsAgregadas(String username) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        List<PartidaStatsDocument> historial = mongoStatsRepository.findByIdUsuario(usuario.getId());
        
        int partidasJugadas = historial.size();
        int hitsAcertados = historial.stream().mapToInt(PartidaStatsDocument::getHitsAcertados).sum();
        int hitsFallados = historial.stream().mapToInt(PartidaStatsDocument::getHitsFallados).sum();
        int barcosHundidos = historial.stream().mapToInt(PartidaStatsDocument::getBarcosHundidos).sum();
        
        // Obtener victorias consultando MongoDB en el historial (partidas donde el usuario es el ganador)
        long partidasGanadas = historial.stream().filter(PartidaStatsDocument::isGanador).count();

        return new StatsAgregadasDTO(
                username, 
                partidasJugadas, 
                (int) partidasGanadas, 
                hitsAcertados, 
                hitsFallados, 
                barcosHundidos
        );
    }

    @Override
    public PartidaStatsDocument guardarStatsPartida(Long idPartida, Long idUsuario, Long idPersonaje, 
                                                     int hitsAcertados, int hitsFallados, int barcosHundidos, 
                                                     String username, boolean ganador) {
        PartidaStatsDocument stats = new PartidaStatsDocument(
                idPartida, idUsuario, idPersonaje, hitsAcertados, hitsFallados, barcosHundidos, username, ganador
        );

        return mongoStatsRepository.save(stats);
    }

    @Override
    public List<PartidaStatsDocument> getHistorial(Long idUsuario) {
        return mongoStatsRepository.findByIdUsuario(idUsuario);
    }

    @Override
    public void actualizarUsernameStats(Long idUsuario, String newUsername) {
        List<PartidaStatsDocument> historial = mongoStatsRepository.findByIdUsuario(idUsuario);
        for (PartidaStatsDocument doc : historial) {
            doc.setUsername(newUsername);
        }
        mongoStatsRepository.saveAll(historial);
    }
}
