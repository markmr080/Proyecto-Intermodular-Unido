package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clase clave compuesta para la entidad PersonajeFlotaEntity.
 * Representa la PK formada por (id_personaje, id_barco_tipo).
 */
public class PersonajeFlotaId implements Serializable {

    private Long personaje;
    private Long barcoTipo;

    public PersonajeFlotaId() {}

    public PersonajeFlotaId(Long personaje, Long barcoTipo) {
        this.personaje = personaje;
        this.barcoTipo = barcoTipo;
    }

    public Long getPersonaje() {
        return personaje;
    }

    public void setPersonaje(Long personaje) {
        this.personaje = personaje;
    }

    public Long getBarcoTipo() {
        return barcoTipo;
    }

    public void setBarcoTipo(Long barcoTipo) {
        this.barcoTipo = barcoTipo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonajeFlotaId)) return false;
        PersonajeFlotaId that = (PersonajeFlotaId) o;
        return Objects.equals(personaje, that.personaje) &&
               Objects.equals(barcoTipo, that.barcoTipo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personaje, barcoTipo);
    }
}
