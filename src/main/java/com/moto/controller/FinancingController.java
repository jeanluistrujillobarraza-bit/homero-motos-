package com.moto.controller;

import com.moto.model.Buyer;
import com.moto.model.FinancingPlan;
import com.moto.model.Motorcycle;
import com.moto.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.moto.repository.FinancingPlanRepository;
import com.moto.repository.MotorcycleRepository;
import com.moto.service.FinancingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/financing")
public class FinancingController {

    private static final Logger log = LoggerFactory.getLogger(FinancingController.class);

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private FinancingService financingService;

    private Motorcycle createFallbackMotorcycle(Double fallbackPrice) {
        Motorcycle m = new Motorcycle();
        m.setMarca("Vehículo");
        m.setModelo("Eliminado");
        m.setPlaca("ELIMINADO");
        m.setCilindraje("-");
        m.setVin("-");
        m.setNumeroMotor("-");
        m.setColor("-");
        m.setPrecioVenta(fallbackPrice != null ? fallbackPrice : 0.0);
        return m;
    }

    private Motorcycle getMotorcycleOrFallback(String id, Double fallbackPrice) {
        if (id == null) {
            return createFallbackMotorcycle(fallbackPrice);
        }
        return motorcycleRepository.findById(id).orElseGet(() -> createFallbackMotorcycle(fallbackPrice));
    }

    @GetMapping
    public String listFinancingPlans(Model model) {
        List<FinancingPlan> plans = financingPlanRepository.findAll();
        // Update statuses in real time
        for (FinancingPlan plan : plans) {
            try {
                financingService.updatePaymentStatus(plan);
                financingPlanRepository.save(plan);
            } catch (Exception e) {
                log.error("Error updating status for plan " + plan.getId(), e);
            }
        }
        
        List<Motorcycle> motos = new ArrayList<>();
        List<String> waUrls = new ArrayList<>();
        for (FinancingPlan plan : plans) {
            Motorcycle m = getMotorcycleOrFallback(plan.getMotorcycleId(), plan.getValorTotal());
            motos.add(m);
            waUrls.add(generateWhatsAppUrl(plan, m));
        }
        
        model.addAttribute("plans", plans);
        model.addAttribute("motos", motos);
        model.addAttribute("waUrls", waUrls);
        return "financing/list";
    }

    @GetMapping({"/new", "/new/"})
    public String showSaleFormWithoutId(@RequestParam(value = "motorcycleId", required = false) String motorcycleId, Model model) {
        if (motorcycleId != null && !motorcycleId.trim().isEmpty()) {
            try {
                return showSaleForm(motorcycleId, model);
            } catch (Exception e) {
                return "redirect:/motorcycles?error=Error+al+buscar+la+motocicleta";
            }
        }
        return "redirect:/motorcycles?error=Debe+seleccionar+una+motocicleta+del+catalogo+para+financiar";
    }

    @GetMapping("/new/{motoId}")
    public String showSaleForm(@PathVariable("motoId") String motoId, Model model) {
        Motorcycle moto = motorcycleRepository.findById(motoId)
                .orElseThrow(() -> new IllegalArgumentException("Motocicleta no encontrada"));
        
        if (!"DISPONIBLE".equals(moto.getEstado())) {
            return "redirect:/motorcycles?error=La+motocicleta+ya+no+esta+disponible";
        }

        model.addAttribute("motorcycle", moto);
        model.addAttribute("buyer", new Buyer());
        model.addAttribute("fechaHoy", LocalDate.now(java.time.ZoneId.of("America/Bogota")).toString());
        return "financing/new";
    }

    @PostMapping("/save")
    public String saveSale(@RequestParam("motorcycleId") String motorcycleId,
                           @RequestParam("nombreCompleto") String nombreCompleto,
                           @RequestParam("cedula") String cedula,
                           @RequestParam("telefono") String telefono,
                           @RequestParam("direccion") String direccion,
                           @RequestParam(value = "correo", required = false) String correo,
                           @RequestParam("fechaCompra") String fechaCompraStr,
                           @RequestParam(value = "buyerObservaciones", required = false) String buyerObservaciones,
                           @RequestParam("valorTotal") Double valorTotal,
                           @RequestParam(value = "cuotaInicial", defaultValue = "0") Double cuotaInicial,
                           @RequestParam("cuotasTotales") Integer cuotasTotales,
                           @RequestParam("valorCuota") Double valorCuota,
                           @RequestParam("frecuenciaPago") String frecuenciaPago,
                           @RequestParam("fechaInicio") String fechaInicioStr,
                           Model model) {
        LocalDate fechaCompra = parseLocalDate(fechaCompraStr);
        LocalDate fechaInicio = parseLocalDate(fechaInicioStr);
        try {
            Buyer buyer = new Buyer(nombreCompleto, cedula, telefono, direccion, correo, fechaCompra, buyerObservaciones);
            FinancingPlan plan = financingService.registerSale(motorcycleId, buyer, valorTotal, cuotaInicial, 
                                                               cuotasTotales, valorCuota, frecuenciaPago, fechaInicio);
            return "redirect:/financing/detail/" + plan.getId();
        } catch (Exception e) {
            log.error("Error al registrar venta de motocicleta", e);
            model.addAttribute("error", "Error al procesar la venta: " + e.getMessage());
            Motorcycle moto = motorcycleRepository.findById(motorcycleId).orElse(null);
            model.addAttribute("motorcycle", moto);
            model.addAttribute("buyer", new Buyer(nombreCompleto, cedula, telefono, direccion, correo, fechaCompra, buyerObservaciones));
            return "financing/new";
        }
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now(java.time.ZoneId.of("America/Bogota"));
        }
        try {
            return LocalDate.parse(dateStr); // Intenta yyyy-MM-dd
        } catch (Exception e) {
            try {
                // Intenta dd/MM/yyyy
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(dateStr, dtf);
            } catch (Exception ex) {
                return LocalDate.now(java.time.ZoneId.of("America/Bogota")); // Fallback a hoy
            }
        }
    }

    @GetMapping("/detail/{id}")
    public String showDetail(@PathVariable("id") String id, Model model) {
        FinancingPlan plan = financingPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plan de financiación no encontrado"));

        // Trigger real-time status check/update based on current date
        financingService.updatePaymentStatus(plan);
        financingPlanRepository.save(plan);

        Motorcycle moto = getMotorcycleOrFallback(plan.getMotorcycleId(), plan.getValorTotal());

        List<Payment> payments = financingService.getPaymentsForPlan(plan.getId());

        model.addAttribute("plan", plan);
        model.addAttribute("motorcycle", moto);
        model.addAttribute("payments", payments);
        model.addAttribute("waUrl", generateWhatsAppUrl(plan, moto));
        return "financing/detail";
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "query", required = false) String query, Model model) {
        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("error", "Debe ingresar un criterio de búsqueda.");
            return "financing/search_results";
        }

        query = query.trim();

        // 1. Search by Placa in Motorcycles
        Optional<Motorcycle> motoOpt = motorcycleRepository.findByPlaca(query.toUpperCase());
        if (motoOpt.isPresent()) {
            Optional<FinancingPlan> planOpt = financingPlanRepository.findByMotorcycleId(motoOpt.get().getId());
            if (planOpt.isPresent()) {
                return "redirect:/financing/detail/" + planOpt.get().getId();
            } else {
                model.addAttribute("info", "La motocicleta con placa " + query + " está registrada pero no cuenta con plan de financiación activo (Estado: " + motoOpt.get().getEstado() + ").");
                model.addAttribute("motorcycle", motoOpt.get());
                return "financing/search_results";
            }
        }

        // 2. Search by Cedula or Name in Financing Plans
        List<FinancingPlan> plans = financingPlanRepository.findByBuyerCedulaContainingIgnoreCaseOrBuyerNombreCompletoContainingIgnoreCase(query, query);
        
        if (plans.size() == 1) {
            return "redirect:/financing/detail/" + plans.get(0).getId();
        } else if (plans.isEmpty()) {
            model.addAttribute("error", "No se encontraron motocicletas ni financiamientos para: " + query);
            return "financing/search_results";
        } else {
            model.addAttribute("results", plans);
            model.addAttribute("query", query);
            
            // Map motorcycles for presentation
            List<Motorcycle> motos = new ArrayList<>();
            List<String> waUrls = new ArrayList<>();
            for (FinancingPlan plan : plans) {
                Motorcycle m = getMotorcycleOrFallback(plan.getMotorcycleId(), plan.getValorTotal());
                motos.add(m);
                waUrls.add(generateWhatsAppUrl(plan, m));
            }
            model.addAttribute("motos", motos);
            model.addAttribute("waUrls", waUrls);
            return "financing/search_results";
        }
    }

    private String generateWhatsAppUrl(FinancingPlan plan, Motorcycle moto) {
        if (plan == null || plan.getBuyer() == null) {
            return "";
        }
        
        String clientName = plan.getBuyer().getNombreCompleto();
        String motoName = moto.getMarca() + " " + moto.getModelo();
        int overdueInstallmentNum = plan.getCuotasPagadas() + 1;
        
        LocalDate dueDate = com.moto.service.FinancingService.getDueDateOfInstallment(plan.getFechaInicio(), overdueInstallmentNum, plan.getFrecuenciaPago());
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dueDateStr = dueDate.format(dtf);
        
        long daysLate = plan.getDiasRetraso() != null ? plan.getDiasRetraso() : 0L;
        
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale("es", "CO"));
        String pendingValueStr = "$" + nf.format(plan.getValorTotalAdeudado() != null ? plan.getValorTotalAdeudado().longValue() : 0L);
        
        String message = "Hola, " + clientName + ". Te recordamos que la cuota #" + overdueInstallmentNum + 
                         " de tu moto " + motoName + " se encuentra vencida desde el " + dueDateStr + 
                         ". Actualmente tienes " + daysLate + " días de retraso y un saldo pendiente de " + pendingValueStr + 
                         ". Por favor, realiza tu pago lo antes posible para evitar inconvenientes. Gracias.";
        
        String phone = plan.getBuyer().getTelefono();
        if (phone != null) {
            phone = phone.replaceAll("\\D", "");
            if (phone.length() == 10) {
                phone = "57" + phone;
            }
        } else {
            phone = "";
        }
        
        try {
            return "https://api.whatsapp.com/send?phone=" + phone + "&text=" + java.net.URLEncoder.encode(message, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return "https://api.whatsapp.com/send?phone=" + phone + "&text=" + org.springframework.web.util.UriUtils.encode(message, "UTF-8");
        }
    }

    @Autowired
    private com.moto.service.ReportService reportService;

    @GetMapping("/export/excel")
    public org.springframework.http.ResponseEntity<byte[]> exportExcel() {
        try {
            List<FinancingPlan> plans = financingPlanRepository.findAll();
            // Update statuses in real time before exporting
            for (FinancingPlan plan : plans) {
                try {
                    financingService.updatePaymentStatus(plan);
                    financingPlanRepository.save(plan);
                } catch (Exception e) {
                    log.error("Error updating status for plan " + plan.getId(), e);
                }
            }

            byte[] excelBytes = reportService.exportFinancingPlansToExcel(plans);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("cartera_creditos.xlsx").build());

            return new org.springframework.http.ResponseEntity<>(excelBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error exporting financing plans", e);
            return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
