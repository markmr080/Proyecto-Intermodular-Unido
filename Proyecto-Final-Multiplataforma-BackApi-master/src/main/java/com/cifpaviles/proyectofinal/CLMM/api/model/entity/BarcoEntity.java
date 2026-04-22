package com.cifpaviles.proyectofinal.CLMM.api.model.entity;


import jakarta.persistence.*;

@Entity
@Table(name = "barcos")
public class BarcoEntity {

    public BarcoEntity() {

    }

    public BarcoEntity(String tipoBarco, int cuantoAtaque, int cuantaDefensa) {
        this.tipoBarco = tipoBarco;
        this.cuantoAtaque = cuantoAtaque;
        this.cuantaDefensa = cuantaDefensa;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String tipoBarco;

    @Column(length = 10, nullable = false)
    private int cuantoAtaque;

    @Column(length = 10, nullable = false)
    private int cuantaDefensa;


    public String getTipoBarco() {
        return tipoBarco;
    }

    public void setTipoBarco(String tipoBarco) {
        this.tipoBarco = tipoBarco;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public int getCuantoAtaque() {
        return cuantoAtaque;
    }

    public void setCuantoAtaque(int cuantoAtaque) {
        this.cuantoAtaque = cuantoAtaque;
    }

    public int getCuantaDefensa() {
        return cuantaDefensa;
    }

    public void setCuantaDefensa(int cuantaDefensa) {
        this.cuantaDefensa = cuantaDefensa;
    }
}

