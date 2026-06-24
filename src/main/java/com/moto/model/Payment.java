package com.moto.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    
    @Indexed
    private String financingPlanId;
    
    private LocalDateTime fechaPago;
    private Double valorPagado;
    private Integer numeroCuota;
    private String metodoPago; // EFECTIVO, TRANSFERENCIA, TARJETA, OTRO
    private String observaciones;
    private String registradoPor;

    // Constructors
    public Payment() {}

    public Payment(String financingPlanId, LocalDateTime fechaPago, Double valorPagado, Integer numeroCuota, 
                   String metodoPago, String observaciones, String registradoPor) {
        this.financingPlanId = financingPlanId;
        this.fechaPago = fechaPago;
        this.valorPagado = valorPagado;
        this.numeroCuota = numeroCuota;
        this.metodoPago = metodoPago;
        this.observaciones = observaciones;
        this.registradoPor = registradoPor;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFinancingPlanId() { return financingPlanId; }
    public void setFinancingPlanId(String financingPlanId) { this.financingPlanId = financingPlanId; }

    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }

    public Double getValorPagado() { return valorPagado; }
    public void setValorPagado(Double valorPagado) { this.valorPagado = valorPagado; }

    public Integer getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(Integer numeroCuota) { this.numeroCuota = numeroCuota; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
}
