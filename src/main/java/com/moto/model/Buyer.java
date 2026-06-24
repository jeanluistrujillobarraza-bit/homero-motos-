package com.moto.model;

import java.time.LocalDate;

public class Buyer {
    private String nombreCompleto;
    private String cedula;
    private String telefono;
    private String direccion;
    private String correo;
    private LocalDate fechaCompra;
    private String observaciones;

    // Constructors
    public Buyer() {}

    public Buyer(String nombreCompleto, String cedula, String telefono, String direccion, String correo, LocalDate fechaCompra, String observaciones) {
        this.nombreCompleto = nombreCompleto;
        this.cedula = cedula;
        this.telefono = telefono;
        this.direccion = direccion;
        this.correo = correo;
        this.fechaCompra = fechaCompra;
        this.observaciones = observaciones;
    }

    // Getters and Setters
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public LocalDate getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(LocalDate fechaCompra) { this.fechaCompra = fechaCompra; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
