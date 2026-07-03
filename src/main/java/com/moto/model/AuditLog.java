package com.moto.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    private String username;
    private String accion;
    private String detalles;
    private LocalDateTime fecha;

    private String ipAddress;
    private String deviceDetails;

    // Constructors
    public AuditLog() {}

    public AuditLog(String username, String accion, String detalles, LocalDateTime fecha) {
        this.username = username;
        this.accion = accion;
        this.detalles = detalles;
        this.fecha = fecha;
    }

    public AuditLog(String username, String accion, String detalles, LocalDateTime fecha, String ipAddress, String deviceDetails) {
        this.username = username;
        this.accion = accion;
        this.detalles = detalles;
        this.fecha = fecha;
        this.ipAddress = ipAddress;
        this.deviceDetails = deviceDetails;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getDetalles() { return detalles; }
    public void setDetalles(String detalles) { this.detalles = detalles; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDeviceDetails() { return deviceDetails; }
    public void setDeviceDetails(String deviceDetails) { this.deviceDetails = deviceDetails; }
}
