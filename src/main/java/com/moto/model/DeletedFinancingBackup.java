package com.moto.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "deleted_financing_backups")
public class DeletedFinancingBackup {
    @Id
    private String id;
    private String motorcycleId;
    private String motorcyclePlaca;
    private String motorcycleMarcaModelo;
    private String originalMotorcycleEstado;
    private FinancingPlan financingPlan;
    private List<Payment> payments;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private String tenantId;

    public DeletedFinancingBackup() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMotorcycleId() { return motorcycleId; }
    public void setMotorcycleId(String motorcycleId) { this.motorcycleId = motorcycleId; }

    public String getMotorcyclePlaca() { return motorcyclePlaca; }
    public void setMotorcyclePlaca(String motorcyclePlaca) { this.motorcyclePlaca = motorcyclePlaca; }

    public String getMotorcycleMarcaModelo() { return motorcycleMarcaModelo; }
    public void setMotorcycleMarcaModelo(String motorcycleMarcaModelo) { this.motorcycleMarcaModelo = motorcycleMarcaModelo; }

    public String getOriginalMotorcycleEstado() { return originalMotorcycleEstado; }
    public void setOriginalMotorcycleEstado(String originalMotorcycleEstado) { this.originalMotorcycleEstado = originalMotorcycleEstado; }

    public FinancingPlan getFinancingPlan() { return financingPlan; }
    public void setFinancingPlan(FinancingPlan financingPlan) { this.financingPlan = financingPlan; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
