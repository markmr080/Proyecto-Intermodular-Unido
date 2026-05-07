package com.cifpaviles.proyectofinal.CLMM.api.service.impl;

import com.cifpaviles.proyectofinal.CLMM.api.model.dto.PersonajeDTO;
import com.cifpaviles.proyectofinal.CLMM.api.model.dto.PersonajeFlotaDTO;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeEntity;
import com.cifpaviles.proyectofinal.CLMM.api.model.entity.PersonajeFlotaEntity;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeFlotaRepository;
import com.cifpaviles.proyectofinal.CLMM.api.repository.mysql.PersonajeRepository;
import com.cifpaviles.proyectofinal.CLMM.api.service.interfaces.PersonajeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PersonajeServiceImpl implements PersonajeService {

    @Autowired
    private PersonajeRepository personajeRepository;

    @Autowired
    private PersonajeFlotaRepository personajeFlotaRepository;

    @Override
    public List<PersonajeDTO> getAllPersonajes() {
        List<PersonajeEntity> personajes = personajeRepository.findAll();
        List<PersonajeDTO> dtos = new ArrayList<>();

        for (PersonajeEntity p : personajes) {
            List<PersonajeFlotaEntity> flotaEntities = personajeFlotaRepository.findByPersonaje(p);
            List<PersonajeFlotaDTO> flotaDTOs = flotaEntities.stream()
                    .map(f -> new PersonajeFlotaDTO(f.getBarcoTipo().getNombre(), f.getBarcoTipo().getTamano(), f.getCantidad()))
                    .collect(Collectors.toList());

            dtos.add(new PersonajeDTO(p.getId(), p.getNombre(), flotaDTOs));
        }

        return dtos;
    }
}
