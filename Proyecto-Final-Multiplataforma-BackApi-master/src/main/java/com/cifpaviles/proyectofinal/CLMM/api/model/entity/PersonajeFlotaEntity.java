package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa la tabla intermedia PERSONAJE_FLOTA.
 * Relaciona un personaje con los tipos de barco que tiene en su flota
 * y la cantidad de cada tipo.
 * 
 * Clave primaria compuesta: (id_personaje, id_barco_tipo)
 */
@Entity
@Table(name = "personaje_flota")
@IdClass(PersonajeFlotaId.class)
public class PersonajeFlotaEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personaje", nullable = false)
    private PersonajeEntity personaje;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_barco_tipo", nullable = false)
    private BarcosCatalogoEntity barcoTipo;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    // -------------------------------------------------------
    //  Constructor sin parámetros requerido por JPA/Hibernate
    // -------------------------------------------------------
    public PersonajeFlotaEntity() {}

    public PersonajeFlotaEntity(PersonajeEntity personaje, BarcosCatalogoEntity barcoTipo, int cantidad) {
        this.personaje = personaje;
        this.barcoTipo = barcoTipo;
        this.cantidad = cantidad;
    }

    // -------------------------------------------------------
    //  Getters y Setters
    // -------------------------------------------------------

    public PersonajeEntity getPersonaje() {
        return personaje;
    }

    public void setPersonaje(PersonajeEntity personaje) {
        this.personaje = personaje;
    }

    public BarcosCatalogoEntity getBarcoTipo() {
        return barcoTipo;
    }

    public void setBarcoTipo(BarcosCatalogoEntity barcoTipo) {
        this.barcoTipo = barcoTipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}
