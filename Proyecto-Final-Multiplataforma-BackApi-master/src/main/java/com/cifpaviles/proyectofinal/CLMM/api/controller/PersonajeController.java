package com.cifpaviles.proyectofinal.CLMM.api.controller;

import com.cifpaviles.proyectofinal.CLMM.api.model.dto.PersonajeDTO;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.PersonajeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/personajes")
public class PersonajeController {

    @Autowired
    private PersonajeService personajeService;

    @GetMapping
    public ResponseEntity<List<PersonajeDTO>> getAllPersonajes() {
        return ResponseEntity.ok(personajeService.getAllPersonajes());
    }
}
