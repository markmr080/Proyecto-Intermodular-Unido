package com.cifpaviles.proyectofinal.CLMM.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PartidaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.repository.PartidaRepository;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/partidas")
public class PartidaController {

    @Autowired
    private PartidaRepository partidaRepository;

    @GetMapping
    public List<PartidaEntity> listarPartidas() {
        return partidaRepository.findAll();
    }

    @PostMapping
    public PartidaEntity crearPartida(@RequestBody PartidaEntity partida) {
        return partidaRepository.save(partida);
    }

    @DeleteMapping("/{id}")
    public void eliminarPartida(@PathVariable Long id) {
        partidaRepository.deleteById(id);
    }
}
