package com.cifpaviles.proyectofinal.CLMM.api.service.impl;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaStatsDocument;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.UsuarioEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.repository.PartidaRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mongo.EstadisticasRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.UsuarioRepository;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEstadisticasService;
import com.cifpaviles.proyectofinal.CLMM.middleware.model.dto.StatsAgregadasDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadisticasService implements IEstadisticasService {

    private final EstadisticasRepository mongoStatsRepository;
    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;

    public EstadisticasService(EstadisticasRepository mongoStatsRepository, 
                               PartidaRepository partidaRepository,
                               UsuarioRepository usuarioRepository) {
        this.mongoStatsRepository = mongoStatsRepository;
        this.partidaRepository = partidaRepository;
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
        
        // Obtener victorias consultando MySQL (partidas donde el usuario es el ganador)
        List<PartidaEntity> victorias = partidaRepository.findByGanador(usuario);
        int partidasGanadas = victorias.size();

        return new StatsAgregadasDTO(
                username, 
                partidasJugadas, 
                partidasGanadas, 
                hitsAcertados, 
                hitsFallados, 
                barcosHundidos
        );
    }

    @Override
    public PartidaStatsDocument guardarStatsPartida(Long idPartida, Long idUsuario, Long idPersonaje, 
                                                     int hitsAcertados, int hitsFallados, int barcosHundidos, 
                                                     String username) {
        PartidaStatsDocument stats = new PartidaStatsDocument(
                idPartida, idUsuario, idPersonaje, hitsAcertados, hitsFallados, barcosHundidos, username
        );
        return mongoStatsRepository.save(stats);
    }

    @Override
    public List<PartidaStatsDocument> getHistorial(Long idUsuario) {
        return mongoStatsRepository.findByIdUsuario(idUsuario);
    }
}
