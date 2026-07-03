package com.moto.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "motorcycles")
public class Motorcycle {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String placa;
    
    private String marca;
    private String modelo;
    private Integer anio;
    private String color;
    private String cilindraje;
    
    @Indexed(unique = true)
    private String vin;
    
    @Indexed(unique = true)
    private String numeroMotor;
    
    private String estado; // DISPONIBLE, VENDIDA, EN_FINANCIACION, PAGADA
    private Double precioVenta;
    private String observaciones;
    private String fotoBase64; // Almacena la foto en Base64

    // Constructors
    public Motorcycle() {}

    public Motorcycle(String placa, String marca, String modelo, Integer anio, String color, 
                      String cilindraje, String vin, String numeroMotor, Double precioVenta, String observaciones) {
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.color = color;
        this.cilindraje = cilindraje;
        this.vin = vin;
        this.numeroMotor = numeroMotor;
        this.precioVenta = precioVenta;
        this.observaciones = observaciones;
        this.estado = "DISPONIBLE";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa != null ? placa.toUpperCase().trim() : null; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getCilindraje() { return cilindraje; }
    public void setCilindraje(String cilindraje) { this.cilindraje = cilindraje; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin != null ? vin.toUpperCase().trim() : null; }

    public String getNumeroMotor() { return numeroMotor; }
    public void setNumeroMotor(String numeroMotor) { this.numeroMotor = numeroMotor != null ? numeroMotor.toUpperCase().trim() : null; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Double getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(Double precioVenta) { this.precioVenta = precioVenta; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getFotoBase64() { return fotoBase64; }
    public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }

    private boolean deleted = false;
    private boolean destacado = false;
    private boolean hidden = false;

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public boolean isDestacado() { return destacado; }
    public void setDestacado(boolean destacado) { this.destacado = destacado; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
}
