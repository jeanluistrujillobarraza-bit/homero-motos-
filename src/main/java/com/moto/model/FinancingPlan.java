package com.moto.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDate;

@Document(collection = "financing_plans")
public class FinancingPlan {
    @Id
    private String id;
    
    @Indexed
    private String motorcycleId;
    
    private Buyer buyer;
    
    private Double valorTotal;
    private Double cuotaInicial;
    private Double saldoFinanciado;
    private Integer cuotasTotales;
    private Double valorCuota;
    private String frecuenciaPago; // DIARIA, SEMANAL, QUINCENAL, MENSUAL
    private LocalDate fechaInicio;
    private LocalDate fechaFinEstimada;
    
    private Double totalPagado = 0.0;
    private Double saldoPendiente = 0.0;
    private Integer cuotasPagadas = 0;
    private Integer cuotasRestantes = 0;
    private Double porcentajeCancelado = 0.0;
    private LocalDate fechaFinActualizada;
    private String estadoCredito = "AL_DIA"; // AL_DIA, ATRASADO, PAGADO
    
    private Integer cuotasAtrasadas = 0;
    private Long diasRetraso = 0L;
    private Double valorTotalAdeudado = 0.0;

    // Constructors
    public FinancingPlan() {}

    public FinancingPlan(String motorcycleId, Buyer buyer, Double valorTotal, Double cuotaInicial, 
                         Integer cuotasTotales, Double valorCuota, String frecuenciaPago, LocalDate fechaInicio) {
        this.motorcycleId = motorcycleId;
        this.buyer = buyer;
        this.valorTotal = valorTotal;
        this.cuotaInicial = cuotaInicial != null ? cuotaInicial : 0.0;
        this.saldoFinanciado = valorTotal - this.cuotaInicial;
        this.cuotasTotales = cuotasTotales;
        this.valorCuota = valorCuota;
        this.frecuenciaPago = frecuenciaPago;
        this.fechaInicio = fechaInicio;
        this.totalPagado = this.cuotaInicial;
        this.saldoPendiente = this.saldoFinanciado;
        this.cuotasPagadas = 0;
        this.cuotasRestantes = cuotasTotales;
        this.porcentajeCancelado = (this.totalPagado / this.valorTotal) * 100.0;
        
        // Calculate estimated end date based on frequency
        this.fechaFinEstimada = calculateEstimatedEndDate(fechaInicio, cuotasTotales, frecuenciaPago);
        this.fechaFinActualizada = this.fechaFinEstimada;
        
        if (this.saldoPendiente <= 0) {
            this.estadoCredito = "PAGADO";
        } else {
            this.estadoCredito = "AL_DIA";
        }
    }

    public static LocalDate calculateEstimatedEndDate(LocalDate start, int totalInstallments, String frequency) {
        if (start == null) return LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        switch (frequency.toUpperCase()) {
            case "DIARIA":
                return start.plusDays(totalInstallments);
            case "SEMANAL":
                return start.plusWeeks(totalInstallments);
            case "QUINCENAL":
                return start.plusWeeks(totalInstallments * 2L);
            case "MENSUAL":
            default:
                return start.plusMonths(totalInstallments);
        }
    }

    public void recalculate(Double additionalPayment) {
        if (additionalPayment != null && additionalPayment > 0) {
            this.totalPagado += additionalPayment;
        }
        this.saldoPendiente = this.valorTotal - this.totalPagado;
        if (this.saldoPendiente < 0) {
            this.saldoPendiente = 0.0;
        }
        
        // Calculate how many installments have been paid based on total paid minus down payment
        double amountPaidTowardsInstallments = this.totalPagado - this.cuotaInicial;
        if (amountPaidTowardsInstallments < 0) {
            amountPaidTowardsInstallments = 0.0;
        }
        
        // Let's calculate based on quota value
        if (this.valorCuota != null && this.valorCuota > 0) {
            this.cuotasPagadas = (int) (amountPaidTowardsInstallments / this.valorCuota);
            if (this.cuotasPagadas > this.cuotasTotales) {
                this.cuotasPagadas = this.cuotasTotales;
            }
        } else {
            this.cuotasPagadas = 0;
        }
        this.cuotasRestantes = this.cuotasTotales - this.cuotasPagadas;
        this.porcentajeCancelado = (this.totalPagado / this.valorTotal) * 100.0;
        if (this.porcentajeCancelado > 100.0) {
            this.porcentajeCancelado = 100.0;
        }
        
        if (this.saldoPendiente <= 0.0) {
            this.estadoCredito = "PAGADO";
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMotorcycleId() { return motorcycleId; }
    public void setMotorcycleId(String motorcycleId) { this.motorcycleId = motorcycleId; }

    public Buyer getBuyer() { return buyer; }
    public void setBuyer(Buyer buyer) { this.buyer = buyer; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }

    public Double getCuotaInicial() { return cuotaInicial; }
    public void setCuotaInicial(Double cuotaInicial) { this.cuotaInicial = cuotaInicial; }

    public Double getSaldoFinanciado() { return saldoFinanciado; }
    public void setSaldoFinanciado(Double saldoFinanciado) { this.saldoFinanciado = saldoFinanciado; }

    public Integer getCuotasTotales() { return cuotasTotales; }
    public void setCuotasTotales(Integer cuotasTotales) { this.cuotasTotales = cuotasTotales; }

    public Double getValorCuota() { return valorCuota; }
    public void setValorCuota(Double valorCuota) { this.valorCuota = valorCuota; }

    public String getFrecuenciaPago() { return frecuenciaPago; }
    public void setFrecuenciaPago(String frecuenciaPago) { this.frecuenciaPago = frecuenciaPago; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFinEstimada() { return fechaFinEstimada; }
    public void setFechaFinEstimada(LocalDate fechaFinEstimada) { this.fechaFinEstimada = fechaFinEstimada; }

    public Double getTotalPagado() { return totalPagado; }
    public void setTotalPagado(Double totalPagado) { this.totalPagado = totalPagado; }

    public Double getSaldoPendiente() { return saldoPendiente; }
    public void setSaldoPendiente(Double saldoPendiente) { this.saldoPendiente = saldoPendiente; }

    public Integer getCuotasPagadas() { return cuotasPagadas; }
    public void setCuotasPagadas(Integer cuotasPagadas) { this.cuotasPagadas = cuotasPagadas; }

    public Integer getCuotasRestantes() { return cuotasRestantes; }
    public void setCuotasRestantes(Integer cuotasRestantes) { this.cuotasRestantes = cuotasRestantes; }

    public Double getPorcentajeCancelado() { return porcentajeCancelado; }
    public void setPorcentajeCancelado(Double porcentajeCancelado) { this.porcentajeCancelado = porcentajeCancelado; }

    public LocalDate getFechaFinActualizada() { return fechaFinActualizada; }
    public void setFechaFinActualizada(LocalDate fechaFinActualizada) { this.fechaFinActualizada = fechaFinActualizada; }

    public String getEstadoCredito() { return estadoCredito; }
    public void setEstadoCredito(String estadoCredito) { this.estadoCredito = estadoCredito; }

    public Integer getCuotasAtrasadas() { return cuotasAtrasadas; }
    public void setCuotasAtrasadas(Integer cuotasAtrasadas) { this.cuotasAtrasadas = cuotasAtrasadas; }

    public Long getDiasRetraso() { return diasRetraso; }
    public void setDiasRetraso(Long diasRetraso) { this.diasRetraso = diasRetraso; }

    public Double getValorTotalAdeudado() { return valorTotalAdeudado; }
    public void setValorTotalAdeudado(Double valorTotalAdeudado) { this.valorTotalAdeudado = valorTotalAdeudado; }
}
