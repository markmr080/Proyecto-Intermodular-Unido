package com.cifpaviles.proyectofinal.Middleware_clmm.middleware.model.dto;

import java.util.List;

public class PersonajeDTO {
    private Long id;
    private String nombre;
    private List<PersonajeFlotaDTO> flota;

    public PersonajeDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<PersonajeFlotaDTO> getFlota() { return flota; }
    public void setFlota(List<PersonajeFlotaDTO> flota) { this.flota = flota; }
}
