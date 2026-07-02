package com.moto.controller;

import com.moto.model.FinancingPlan;
import com.moto.model.Motorcycle;
import com.moto.model.Payment;
import com.moto.repository.AuditLogRepository;
import com.moto.repository.FinancingPlanRepository;
import com.moto.repository.MotorcycleRepository;
import com.moto.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private com.moto.service.FinancingService financingService;

    @GetMapping("/")
    public String dashboard(Model model) {
        // Motorcycle stats
        List<Motorcycle> allMotos = motorcycleRepository.findAll();
        long totalMotos = allMotos.size();
        long disponibles = allMotos.stream().filter(m -> "DISPONIBLE".equals(m.getEstado())).count();
        long vendidas = allMotos.stream().filter(m -> "VENDIDA".equals(m.getEstado())).count();
        long enFinanciacion = allMotos.stream().filter(m -> "EN_FINANCIACION".equals(m.getEstado())).count();
        long pagadas = allMotos.stream().filter(m -> "PAGADA".equals(m.getEstado())).count();

        // Financing stats
        List<FinancingPlan> allPlans = financingPlanRepository.findAll();
        for (FinancingPlan plan : allPlans) {
            if (!"PAGADO".equals(plan.getEstadoCredito())) {
                try {
                    financingService.updatePaymentStatus(plan);
                    financingPlanRepository.save(plan);
                } catch (Exception e) {
                    // Ignore or log
                }
            }
        }

        long creditosActivos = allPlans.stream().filter(p -> !"PAGADO".equals(p.getEstadoCredito())).count();
        long creditosFinalizados = allPlans.stream().filter(p -> "PAGADO".equals(p.getEstadoCredito())).count();

        double saldoTotalPendiente = allPlans.stream()
                .mapToDouble(FinancingPlan::getSaldoPendiente)
                .sum();

        // Payments stats
        List<Payment> allPayments = paymentRepository.findAll();
        double dineroTotalRecaudado = allPayments.stream()
                .mapToDouble(Payment::getValorPagado)
                .sum();

        double recaudoEfectivo = allPayments.stream()
                .filter(p -> "EFECTIVO".equalsIgnoreCase(p.getMetodoPago()))
                .mapToDouble(Payment::getValorPagado)
                .sum();

        double recaudoNequi = allPayments.stream()
                .filter(p -> "NEQUI".equalsIgnoreCase(p.getMetodoPago()))
                .mapToDouble(Payment::getValorPagado)
                .sum();

        // Payments today
        LocalDate today = LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        List<Payment> todayPayments = paymentRepository.findByFechaPagoBetween(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay()
        );
        double dineroRecaudadoHoy = todayPayments.stream()
                .mapToDouble(Payment::getValorPagado)
                .sum();

        // Audit Logs & Recent Payments
        model.addAttribute("totalMotos", totalMotos);
        model.addAttribute("disponibles", disponibles);
        model.addAttribute("vendidas", vendidas + enFinanciacion + pagadas); // Total sold
        model.addAttribute("creditosActivos", creditosActivos);
        model.addAttribute("creditosFinalizados", creditosFinalizados);
        model.addAttribute("dineroTotalRecaudado", dineroTotalRecaudado);
        model.addAttribute("recaudoEfectivo", recaudoEfectivo);
        model.addAttribute("recaudoNequi", recaudoNequi);
        model.addAttribute("saldoTotalPendiente", saldoTotalPendiente);
        model.addAttribute("dineroRecaudadoHoy", dineroRecaudadoHoy);
        model.addAttribute("recentLogs", auditLogRepository.findFirst10ByOrderByFechaDesc());
        model.addAttribute("recentPayments", paymentRepository.findFirst10ByOrderByFechaPagoDesc());

        return "dashboard";
    }
}
