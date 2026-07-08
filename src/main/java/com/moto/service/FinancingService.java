package com.moto.service;

import com.moto.model.*;
import com.moto.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class FinancingService {

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditService auditService;

    private String getCurrentTenantId() {
        if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
            Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getTenantId();
            }
        }
        return "default";
    }

    public FinancingPlan registerSale(String motorcycleId, Buyer buyer, Double valorTotal, 
                                      Double cuotaInicial, Integer cuotasTotales, Double valorCuota, 
                                      String frecuenciaPago, LocalDate fechaInicio) {
        
        Motorcycle moto = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new IllegalArgumentException("Motocicleta no encontrada"));

        if (!"DISPONIBLE".equals(moto.getEstado())) {
            throw new IllegalStateException("La motocicleta no está disponible para venta");
        }

        // Create financing plan
        FinancingPlan plan = new FinancingPlan(motorcycleId, buyer, valorTotal, cuotaInicial, 
                                               cuotasTotales, valorCuota, frecuenciaPago, fechaInicio);
        plan.setTenantId(getCurrentTenantId());
        
        // Save financing plan
        plan = financingPlanRepository.save(plan);

        // Update motorcycle state
        if (plan.getSaldoPendiente() <= 0) {
            moto.setEstado("PAGADA");
        } else {
            moto.setEstado("EN_FINANCIACION");
        }
        motorcycleRepository.save(moto);

        // Audit Log
        auditService.log("REGISTRO_VENTA", "Venta registrada para placa: " + moto.getPlaca() + 
                         " a comprador: " + buyer.getNombreCompleto() + ". Saldo financiado: " + plan.getSaldoFinanciado());

        // Check if down payment was registered and add it as initial payment record if > 0
        if (cuotaInicial != null && cuotaInicial > 0) {
            Payment initialPayment = new Payment(plan.getId(), LocalDateTime.now(java.time.ZoneId.of("America/Bogota")), cuotaInicial, 0, 
                                                 "EFECTIVO", "Cuota Inicial de la compra", "SYSTEM");
            initialPayment.setTenantId(getCurrentTenantId());
            paymentRepository.save(initialPayment);
        }

        return plan;
    }

    public Payment registerPayment(String planId, Double valorPagado, String metodoPago, String observaciones) {
        FinancingPlan plan = financingPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan de financiación no encontrado"));

        if ("PAGADO".equals(plan.getEstadoCredito())) {
            throw new IllegalStateException("Este crédito ya ha sido cancelado en su totalidad.");
        }

        int nextInstallmentNumber = plan.getCuotasPagadas() + 1;
        
        Payment payment = new Payment();
        payment.setFinancingPlanId(plan.getId());
        payment.setFechaPago(LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        payment.setValorPagado(valorPagado);
        payment.setNumeroCuota(nextInstallmentNumber);
        payment.setMetodoPago(metodoPago);
        payment.setObservaciones(observaciones);
        
        String username = "SYSTEM";
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (principal != null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        }
        payment.setRegistradoPor(username);
        payment.setTenantId(getCurrentTenantId());
        
        payment = paymentRepository.save(payment);

        // Recalculate financing plan status
        plan.recalculate(valorPagado);
        
        // Check if fully paid
        if (plan.getSaldoPendiente() <= 0.0) {
            plan.setEstadoCredito("PAGADO");
            
            // Update motorcycle status to PAGADA
            Optional<Motorcycle> motoOpt = motorcycleRepository.findById(plan.getMotorcycleId());
            if (motoOpt.isPresent()) {
                Motorcycle moto = motoOpt.get();
                moto.setEstado("PAGADA");
                motorcycleRepository.save(moto);
            }
            
            auditService.log("CREDITO_FINALIZADO", "Crédito finalizado para plan ID: " + plan.getId() + ". Deuda saldada.");
        } else {
            auditService.log("REGISTRO_PAGO", "Pago registrado de $" + valorPagado + " para plan ID: " + plan.getId());
        }

        // Refresh and update state (Al día, atrasado, etc.)
        updatePaymentStatus(plan);
        financingPlanRepository.save(plan);

        return payment;
    }

    public static LocalDate getDueDateOfInstallment(LocalDate startDate, int installmentNum, String frequency) {
        if (startDate == null) return LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        long offset = installmentNum;
        switch (frequency.toUpperCase()) {
            case "DIARIA":
                return startDate.plusDays(offset);
            case "SEMANAL":
                return startDate.plusWeeks(offset);
            case "QUINCENAL":
                return startDate.plusDays(offset * 14);
            case "MENSUAL":
            default:
                return startDate.plusMonths(offset);
        }
    }

    public void updatePaymentStatus(FinancingPlan plan) {
        if ("PAGADO".equals(plan.getEstadoCredito())) {
            plan.setCuotasAtrasadas(0);
            plan.setDiasRetraso(0L);
            plan.setValorTotalAdeudado(0.0);
            return;
        }

        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        int expectedInstallments = 0;
        for (int i = 1; i <= plan.getCuotasTotales(); i++) {
            LocalDate dueDate = getDueDateOfInstallment(plan.getFechaInicio(), i, plan.getFrecuenciaPago());
            if (!today.isBefore(dueDate)) {
                expectedInstallments++;
            } else {
                break;
            }
        }

        int paidInstallments = plan.getCuotasPagadas();
        Optional<Motorcycle> motoOpt = motorcycleRepository.findById(plan.getMotorcycleId());

        if (paidInstallments >= expectedInstallments) {
            plan.setCuotasAtrasadas(0);
            plan.setDiasRetraso(0L);
            plan.setValorTotalAdeudado(0.0);

            // Check if they paid today
            List<Payment> todayPayments = paymentRepository.findByFechaPagoBetween(
                    today.atStartOfDay(), today.plusDays(1).atStartOfDay()
            );
            boolean paidToday = todayPayments.stream()
                    .anyMatch(p -> p.getFinancingPlanId().equals(plan.getId()));
            
            if (paidToday) {
                plan.setEstadoCredito("AL_DIA_PAGO_HOY");
            } else {
                plan.setEstadoCredito("AL_DIA");
            }

            if (motoOpt.isPresent()) {
                Motorcycle moto = motoOpt.get();
                if ("PAGO_VENCIDO".equals(moto.getEstado()) || "DISPONIBLE".equals(moto.getEstado())) {
                    moto.setEstado("EN_FINANCIACION");
                    motorcycleRepository.save(moto);
                }
            }
        } else {
            plan.setEstadoCredito("ATRASADO");
            
            int cuotasAtrasadas = expectedInstallments - paidInstallments;
            plan.setCuotasAtrasadas(cuotasAtrasadas);

            int firstOverdueInstallmentNum = paidInstallments + 1;
            LocalDate firstOverdueDate = getDueDateOfInstallment(plan.getFechaInicio(), firstOverdueInstallmentNum, plan.getFrecuenciaPago());
            long daysLate = ChronoUnit.DAYS.between(firstOverdueDate, today);
            plan.setDiasRetraso(daysLate > 0 ? daysLate : 0L);

            double totalExpectedAmount = plan.getCuotaInicial() + (expectedInstallments * plan.getValorCuota());
            double overdueAmount = totalExpectedAmount - plan.getTotalPagado();
            if (overdueAmount < 0.0) {
                overdueAmount = 0.0;
            }
            if (overdueAmount > plan.getSaldoPendiente()) {
                overdueAmount = plan.getSaldoPendiente();
            }
            plan.setValorTotalAdeudado(overdueAmount);

            if (motoOpt.isPresent()) {
                Motorcycle moto = motoOpt.get();
                if (!"PAGO_VENCIDO".equals(moto.getEstado())) {
                    moto.setEstado("PAGO_VENCIDO");
                    motorcycleRepository.save(moto);
                }
            }
        }
    }

    public List<Payment> getPaymentsForPlan(String planId) {
        return paymentRepository.findByFinancingPlanIdOrderByFechaPagoAsc(planId);
    }

    public void editPayment(String paymentId, Double nuevoValor, String nuevoMetodo, String nuevasObservaciones) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        FinancingPlan plan = financingPlanRepository.findById(payment.getFinancingPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));

        double antiguoValor = payment.getValorPagado();
        payment.setValorPagado(nuevoValor);
        payment.setMetodoPago(nuevoMetodo);
        payment.setObservaciones(nuevasObservaciones);
        paymentRepository.save(payment);

        // Recalcular saldo total pagado del plan sumando todos sus pagos
        recalculatePlanPayments(plan);

        auditService.log("EDICION_PAGO", "Pago ID: " + paymentId + " editado. Valor anterior: $" + antiguoValor + ", Nuevo valor: $" + nuevoValor);
    }

    public void deletePayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        FinancingPlan plan = financingPlanRepository.findById(payment.getFinancingPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));

        paymentRepository.deleteById(paymentId);

        // Recalcular saldo total pagado
        recalculatePlanPayments(plan);

        auditService.log("ELIMINACION_PAGO", "Pago ID: " + paymentId + " de $" + payment.getValorPagado() + " eliminado.");
    }

    private void recalculatePlanPayments(FinancingPlan plan) {
        List<Payment> payments = paymentRepository.findByFinancingPlanIdOrderByFechaPagoAsc(plan.getId());
        double totalPagado = 0.0;
        for (Payment p : payments) {
            totalPagado += p.getValorPagado();
        }

        // Si por alguna razón la cuota inicial no está en los pagos o queremos basarnos en ellos
        plan.setTotalPagado(totalPagado);
        plan.setSaldoPendiente(plan.getValorTotal() - totalPagado);
        if (plan.getSaldoPendiente() < 0.0) {
            plan.setSaldoPendiente(0.0);
        }

        double amountPaidTowardsInstallments = totalPagado - plan.getCuotaInicial();
        if (amountPaidTowardsInstallments < 0.0) {
            amountPaidTowardsInstallments = 0.0;
        }

        if (plan.getValorCuota() != null && plan.getValorCuota() > 0) {
            plan.setCuotasPagadas((int) (amountPaidTowardsInstallments / plan.getValorCuota()));
            if (plan.getCuotasPagadas() > plan.getCuotasTotales()) {
                plan.setCuotasPagadas(plan.getCuotasTotales());
            }
        } else {
            plan.setCuotasPagadas(0);
        }
        plan.setCuotasRestantes(plan.getCuotasTotales() - plan.getCuotasPagadas());
        
        double pct = (totalPagado / plan.getValorTotal()) * 100.0;
        plan.setPorcentajeCancelado(pct > 100.0 ? 100.0 : pct);

        if (plan.getSaldoPendiente() <= 0.0) {
            plan.setEstadoCredito("PAGADO");
            Optional<Motorcycle> motoOpt = motorcycleRepository.findById(plan.getMotorcycleId());
            if (motoOpt.isPresent()) {
                Motorcycle moto = motoOpt.get();
                moto.setEstado("PAGADA");
                motorcycleRepository.save(moto);
            }
        } else {
            plan.setEstadoCredito("AL_DIA"); // Will be updated by updatePaymentStatus
            Optional<Motorcycle> motoOpt = motorcycleRepository.findById(plan.getMotorcycleId());
            if (motoOpt.isPresent()) {
                Motorcycle moto = motoOpt.get();
                if ("PAGADA".equals(moto.getEstado())) {
                    moto.setEstado("EN_FINANCIACION");
                    motorcycleRepository.save(moto);
                }
            }
        }

        updatePaymentStatus(plan);
        financingPlanRepository.save(plan);
    }

    public void updateFinancingPlanAndBuyer(String planId, Buyer newBuyer, 
                                            Double valorTotal, Double cuotaInicial, 
                                            Integer cuotasTotales, Double valorCuota, 
                                            String frecuenciaPago, LocalDate fechaInicio) {
        FinancingPlan plan = financingPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan de financiación no encontrado"));

        // 1. Update Buyer details
        Buyer buyer = plan.getBuyer();
        if (buyer == null) {
            buyer = new Buyer();
        }
        buyer.setNombreCompleto(newBuyer.getNombreCompleto());
        buyer.setCedula(newBuyer.getCedula());
        buyer.setTelefono(newBuyer.getTelefono());
        buyer.setDireccion(newBuyer.getDireccion());
        buyer.setCorreo(newBuyer.getCorreo());
        buyer.setFechaCompra(newBuyer.getFechaCompra());
        buyer.setObservaciones(newBuyer.getObservaciones());
        plan.setBuyer(buyer);

        // 2. Update Plan details
        plan.setValorTotal(valorTotal);
        plan.setCuotaInicial(cuotaInicial != null ? cuotaInicial : 0.0);
        plan.setSaldoFinanciado(valorTotal - plan.getCuotaInicial());
        plan.setCuotasTotales(cuotasTotales);
        plan.setValorCuota(valorCuota);
        plan.setFrecuenciaPago(frecuenciaPago);
        plan.setFechaInicio(fechaInicio);
        plan.setFechaFinEstimada(FinancingPlan.calculateEstimatedEndDate(fechaInicio, cuotasTotales, frecuenciaPago));
        plan.setFechaFinActualizada(plan.getFechaFinEstimada());

        // 3. Update the Cuota Inicial payment record (numeroCuota == 0)
        List<Payment> payments = paymentRepository.findByFinancingPlanIdOrderByFechaPagoAsc(planId);
        Optional<Payment> initialPaymentOpt = payments.stream()
                .filter(p -> p.getNumeroCuota() == 0)
                .findFirst();

        double initialPaymentVal = cuotaInicial != null ? cuotaInicial : 0.0;
        if (initialPaymentOpt.isPresent()) {
            Payment initialPayment = initialPaymentOpt.get();
            if (initialPaymentVal > 0) {
                initialPayment.setValorPagado(initialPaymentVal);
                paymentRepository.save(initialPayment);
            } else {
                paymentRepository.delete(initialPayment);
            }
        } else if (initialPaymentVal > 0) {
            Payment initialPayment = new Payment(planId, LocalDateTime.now(java.time.ZoneId.of("America/Bogota")), initialPaymentVal, 0, 
                                                 "EFECTIVO", "Cuota Inicial de la compra", "SYSTEM");
            initialPayment.setTenantId(plan.getTenantId());
            paymentRepository.save(initialPayment);
        }

        // 4. Recalculate plan payments & status
        recalculatePlanPayments(plan);
        
        auditService.log("EDICION_PLAN", "Plan ID: " + planId + " editado por administrador. Datos del comprador y condiciones del plan actualizados.");
    }
}
