package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa la tabla PERSONAJES.
 * Cada personaje tiene un nombre y puede tener una flota de barcos asignada (PERSONAJE_FLOTA).
 */
@Entity
@Table(name = "personajes")
public class PersonajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_personaje")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // -------------------------------------------------------
    //  Constructor sin parámetros requerido por JPA/Hibernate
    // -------------------------------------------------------
    public PersonajeEntity() {}

    public PersonajeEntity(String nombre) {
        this.nombre = nombre;
    }

    // -------------------------------------------------------
    //  Getters y Setters
    // -------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
