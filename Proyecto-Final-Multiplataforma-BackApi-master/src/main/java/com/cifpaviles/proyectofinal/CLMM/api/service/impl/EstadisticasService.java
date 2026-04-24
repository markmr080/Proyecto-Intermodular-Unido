package com.cifpaviles.proyectofinal.CLMM.api.service.impl;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidasStatsEntity;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PartidasStatsRepository;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.IEstadisticasService;
import org.springframework.stereotype.Service;

@Service
public class EstadisticasService implements IEstadisticasService {

    private final PartidasStatsRepository statsRepository;

    public EstadisticasService(PartidasStatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    @Override
    public PartidasStatsEntity getStats(String nickname) {
        // Si el jugador todavía no tiene registro, lo creamos con todo a 0
        return statsRepository.findByNickname(nickname)
                .orElseGet(() -> {
                    PartidasStatsEntity nuevo = new PartidasStatsEntity(nickname);
                    return statsRepository.save(nuevo);
                });
    }

    @Override
    public PartidasStatsEntity actualizarStats(String nickname, boolean ganada, int impactosAcertados, int impactosFallados) {
        // Obtenemos (o creamos) el registro existente
        PartidasStatsEntity stats = getStats(nickname);

        // Acumulamos los nuevos valores
        if (ganada) {
            stats.setPartidasGanadas(stats.getPartidasGanadas() + 1);
        }
        stats.setImpactosAcertados(stats.getImpactosAcertados() + impactosAcertados);
        stats.setImpactosFallados(stats.getImpactosFallados() + impactosFallados);

        return statsRepository.save(stats);
    }
}
