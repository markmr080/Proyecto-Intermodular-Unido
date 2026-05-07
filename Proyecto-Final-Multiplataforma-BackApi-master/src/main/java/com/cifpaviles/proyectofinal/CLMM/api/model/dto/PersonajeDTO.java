package com.cifpaviles.proyectofinal.CLMM.api.model.dto;

import java.util.List;

public class PersonajeDTO {
    private Long id;
    private String nombre;
    private List<PersonajeFlotaDTO> flota;

    public PersonajeDTO(Long id, String nombre, List<PersonajeFlotaDTO> flota) {
        this.id = id;
        this.nombre = nombre;
        this.flota = flota;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<PersonajeFlotaDTO> getFlota() { return flota; }
    public void setFlota(List<PersonajeFlotaDTO> flota) { this.flota = flota; }
}
