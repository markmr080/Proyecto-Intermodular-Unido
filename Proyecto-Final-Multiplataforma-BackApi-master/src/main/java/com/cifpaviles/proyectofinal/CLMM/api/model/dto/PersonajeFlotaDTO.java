package com.cifpaviles.proyectofinal.CLMM.api.model.dto;

public class PersonajeFlotaDTO {
    private String tipoBarco;
    private int tamano;
    private int cantidad;

    public PersonajeFlotaDTO(String tipoBarco, int tamano, int cantidad) {
        this.tipoBarco = tipoBarco;
        this.tamano = tamano;
        this.cantidad = cantidad;
    }

    public String getTipoBarco() { return tipoBarco; }
    public void setTipoBarco(String tipoBarco) { this.tipoBarco = tipoBarco; }
    public int getTamano() { return tamano; }
    public void setTamano(int tamano) { this.tamano = tamano; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
