package com.cifpaviles.proyectofinal.CLMM.api.model.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa la tabla BARCOS_CATALOGO.
 * Reemplaza a la antigua BarcoEntity, simplificando el modelo:
 * solo contiene el nombre del tipo de barco y su tamaño (nº de celdas).
 */
@Entity
@Table(name = "barcos_catalogo")
public class BarcosCatalogoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_barco_tipo")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "tamano", nullable = false)
    private int tamano;

    // -------------------------------------------------------
    //  Constructor sin parámetros requerido por JPA/Hibernate
    // -------------------------------------------------------
    public BarcosCatalogoEntity() {}

    public BarcosCatalogoEntity(String nombre, int tamano) {
        this.nombre = nombre;
        this.tamano = tamano;
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

    public int getTamano() {
        return tamano;
    }

    public void setTamano(int tamano) {
        this.tamano = tamano;
    }
}
