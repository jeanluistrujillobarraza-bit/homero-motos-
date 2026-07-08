package com.moto.controller;

import com.moto.model.FinancingPlan;
import com.moto.model.Motorcycle;
import com.moto.model.Payment;
import com.moto.repository.FinancingPlanRepository;
import com.moto.repository.MotorcycleRepository;
import com.moto.repository.PaymentRepository;
import com.moto.service.FinancingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private FinancingService financingService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    public static class PaymentWithDetails {
        private final Payment payment;
        private final String buyerName;
        private final String plate;

        public PaymentWithDetails(Payment payment, String buyerName, String plate) {
            this.payment = payment;
            this.buyerName = buyerName;
            this.plate = plate;
        }

        public Payment getPayment() { return payment; }
        public String getBuyerName() { return buyerName; }
        public String getPlate() { return plate; }
    }

    private String getCurrentTenantId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.moto.service.CustomUserDetails) {
            return ((com.moto.service.CustomUserDetails) auth.getPrincipal()).getTenantId();
        }
        return "default";
    }

    @GetMapping
    public String listPayments(@RequestParam(value = "filter", defaultValue = "TODO") String filter, Model model) {
        String tenantId = getCurrentTenantId();
        List<Payment> all = paymentRepository.findByTenantId(tenantId);
        // Sort by payment date descending
        all.sort((p1, p2) -> p2.getFechaPago().compareTo(p1.getFechaPago()));

        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now(java.time.ZoneId.of("America/Bogota"));
        boolean filterActive = false;
        
        if ("HOY".equalsIgnoreCase(filter)) {
            cutoff = java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")).atStartOfDay();
            filterActive = true;
        } else if ("QUINCENA".equalsIgnoreCase(filter)) {
            cutoff = java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")).minusDays(15).atStartOfDay();
            filterActive = true;
        } else if ("MES".equalsIgnoreCase(filter)) {
            cutoff = java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")).minusMonths(1).atStartOfDay();
            filterActive = true;
        }

        List<PaymentWithDetails> cashPayments = new ArrayList<>();
        List<PaymentWithDetails> nequiPayments = new ArrayList<>();

        for (Payment p : all) {
            // Apply date filtering
            if (filterActive && p.getFechaPago().isBefore(cutoff)) {
                continue;
            }

            FinancingPlan plan = financingPlanRepository.findById(p.getFinancingPlanId()).orElse(null);
            String buyerName = "Cliente Eliminado";
            String plate = "-";
            if (plan != null) {
                buyerName = plan.getBuyer().getNombreCompleto();
                Motorcycle m = motorcycleRepository.findById(plan.getMotorcycleId()).orElse(null);
                if (m != null) {
                    plate = m.getPlaca();
                }
            }

            PaymentWithDetails pwd = new PaymentWithDetails(p, buyerName, plate);
            if ("EFECTIVO".equalsIgnoreCase(p.getMetodoPago())) {
                cashPayments.add(pwd);
            } else if ("NEQUI".equalsIgnoreCase(p.getMetodoPago())) {
                nequiPayments.add(pwd);
            }
        }

        model.addAttribute("cashPayments", cashPayments);
        model.addAttribute("nequiPayments", nequiPayments);
        model.addAttribute("currentFilter", filter.toUpperCase());

        double totalCash = cashPayments.stream().mapToDouble(x -> x.getPayment().getValorPagado()).sum();
        double totalNequi = nequiPayments.stream().mapToDouble(x -> x.getPayment().getValorPagado()).sum();
        model.addAttribute("totalCash", totalCash);
        model.addAttribute("totalNequi", totalNequi);

        return "payments/list";
    }

    @Autowired
    private com.moto.service.PdfReceiptService pdfReceiptService;

    @Autowired
    private com.moto.service.ReportService reportService;

    @GetMapping("/receipt/{id}")
    public org.springframework.http.ResponseEntity<byte[]> downloadReceipt(@org.springframework.web.bind.annotation.PathVariable("id") String paymentId) {
        try {
            String tenantId = getCurrentTenantId();
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
            if (!payment.getTenantId().equals(tenantId)) {
                return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.FORBIDDEN);
            }
            FinancingPlan plan = financingPlanRepository.findById(payment.getFinancingPlanId())
                    .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));
            Motorcycle moto = motorcycleRepository.findById(plan.getMotorcycleId()).orElse(null);

            byte[] pdfBytes = pdfReceiptService.generateReceiptPdf(payment, plan, moto);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.inline().filename("recibo-" + paymentId + ".pdf").build());

            return new org.springframework.http.ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/export/excel")
    public org.springframework.http.ResponseEntity<byte[]> exportExcel() {
        try {
            String tenantId = getCurrentTenantId();
            List<Payment> all = paymentRepository.findByTenantId(tenantId);
            all.sort((p1, p2) -> p2.getFechaPago().compareTo(p1.getFechaPago()));

            List<PaymentWithDetails> allDetails = new ArrayList<>();
            for (Payment p : all) {
                FinancingPlan plan = financingPlanRepository.findById(p.getFinancingPlanId()).orElse(null);
                String buyerName = "Cliente Eliminado";
                String plate = "-";
                if (plan != null) {
                    buyerName = plan.getBuyer().getNombreCompleto();
                    Motorcycle m = motorcycleRepository.findById(plan.getMotorcycleId()).orElse(null);
                    if (m != null) {
                        plate = m.getPlaca();
                    }
                }
                allDetails.add(new PaymentWithDetails(p, buyerName, plate));
            }

            byte[] excelBytes = reportService.exportPaymentsToExcel(allDetails);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("libro_caja.xlsx").build());

            return new org.springframework.http.ResponseEntity<>(excelBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/register")
    public String registerPayment(@RequestParam("planId") String planId,
                                  @RequestParam("valorPagado") Double valorPagado,
                                  @RequestParam("metodoPago") String metodoPago,
                                  @RequestParam(value = "observaciones", required = false) String observaciones,
                                  RedirectAttributes redirectAttributes) {
        try {
            String tenantId = getCurrentTenantId();
            FinancingPlan plan = financingPlanRepository.findById(planId).orElse(null);
            if (plan == null || !plan.getTenantId().equals(tenantId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Acceso denegado");
                return "redirect:/financing";
            }
            financingService.registerPayment(planId, valorPagado, metodoPago, observaciones);
            redirectAttributes.addFlashAttribute("successMessage", "¡Pago registrado exitosamente!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al registrar el pago: " + e.getMessage());
        }
        return "redirect:/financing/detail/" + planId;
    }

    @PostMapping("/edit")
    public String editPayment(@RequestParam("paymentId") String paymentId,
                              @RequestParam("planId") String planId,
                              @RequestParam("valorPagado") Double valorPagado,
                              @RequestParam("metodoPago") String metodoPago,
                              @RequestParam(value = "observaciones", required = false) String observaciones,
                              RedirectAttributes redirectAttributes) {
        try {
            String tenantId = getCurrentTenantId();
            FinancingPlan plan = financingPlanRepository.findById(planId).orElse(null);
            if (plan == null || !plan.getTenantId().equals(tenantId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Acceso denegado");
                return "redirect:/financing";
            }
            financingService.editPayment(paymentId, valorPagado, metodoPago, observaciones);
            redirectAttributes.addFlashAttribute("successMessage", "¡Pago modificado exitosamente!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al modificar el pago: " + e.getMessage());
        }
        return "redirect:/financing/detail/" + planId;
    }

    @PostMapping("/delete")
    public String deletePayment(@RequestParam("paymentId") String paymentId,
                                @RequestParam("planId") String planId,
                                RedirectAttributes redirectAttributes) {
        try {
            String tenantId = getCurrentTenantId();
            FinancingPlan plan = financingPlanRepository.findById(planId).orElse(null);
            if (plan == null || !plan.getTenantId().equals(tenantId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Acceso denegado");
                return "redirect:/financing";
            }
            financingService.deletePayment(paymentId);
            redirectAttributes.addFlashAttribute("successMessage", "¡Pago eliminado exitosamente!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al eliminar el pago: " + e.getMessage());
        }
        return "redirect:/financing/detail/" + planId;
    }
}
