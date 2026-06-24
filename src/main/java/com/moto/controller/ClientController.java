package com.moto.controller;

import com.moto.model.FinancingPlan;
import com.moto.model.Motorcycle;
import com.moto.repository.FinancingPlanRepository;
import com.moto.repository.MotorcycleRepository;
import com.moto.service.FinancingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    @Autowired
    private FinancingService financingService;

    public static class ClientSummary {
        private String nombreCompleto;
        private String cedula;
        private String telefono;
        private String direccion;
        private String correo;
        private int totalCreditos;
        private double saldoTotalPendiente;
        private boolean tieneMora;

        public ClientSummary(String nombre, String cedula, String tel, String dir, String correo) {
            this.nombreCompleto = nombre;
            this.cedula = cedula;
            this.telefono = tel;
            this.direccion = dir;
            this.correo = correo;
            this.totalCreditos = 0;
            this.saldoTotalPendiente = 0.0;
            this.tieneMora = false;
        }

        public String getNombreCompleto() { return nombreCompleto; }
        public String getCedula() { return cedula; }
        public String getTelefono() { return telefono; }
        public String getDireccion() { return direccion; }
        public String getCorreo() { return correo; }
        public int getTotalCreditos() { return totalCreditos; }
        public double getSaldoTotalPendiente() { return saldoTotalPendiente; }
        public boolean isTieneMora() { return tieneMora; }

        public void addCredit(double saldo, String estadoCredito) {
            this.totalCreditos++;
            this.saldoTotalPendiente += saldo;
            if ("ATRASADO".equalsIgnoreCase(estadoCredito)) {
                this.tieneMora = true;
            }
        }
    }

    @GetMapping
    public String listClients(Model model) {
        List<FinancingPlan> allPlans = financingPlanRepository.findAll();
        Map<String, ClientSummary> clientMap = new HashMap<>();

        for (FinancingPlan plan : allPlans) {
            if (plan.getBuyer() == null) continue;
            String cedula = plan.getBuyer().getCedula();
            
            ClientSummary summary = clientMap.computeIfAbsent(cedula, k -> new ClientSummary(
                    plan.getBuyer().getNombreCompleto(),
                    cedula,
                    plan.getBuyer().getTelefono(),
                    plan.getBuyer().getDireccion(),
                    plan.getBuyer().getCorreo()
            ));

            summary.addCredit(plan.getSaldoPendiente(), plan.getEstadoCredito());
        }

        model.addAttribute("clients", new ArrayList<>(clientMap.values()));
        return "clients/list";
    }

    @GetMapping("/detail/{cedula}")
    public String clientDetail(@PathVariable("cedula") String cedula, Model model) {
        List<FinancingPlan> allPlans = financingPlanRepository.findAll();
        List<FinancingPlan> clientPlans = new ArrayList<>();
        List<Motorcycle> clientMotos = new ArrayList<>();

        FinancingPlan representativePlan = null;

        for (FinancingPlan plan : allPlans) {
            if (plan.getBuyer() != null && cedula.equalsIgnoreCase(plan.getBuyer().getCedula())) {
                representativePlan = plan;
                // Force update status before viewing client profile
                financingService.updatePaymentStatus(plan);
                financingPlanRepository.save(plan);

                clientPlans.add(plan);
                Motorcycle m = motorcycleRepository.findById(plan.getMotorcycleId()).orElse(null);
                if (m == null) {
                    m = new Motorcycle();
                    m.setMarca("Eliminada");
                    m.setModelo("-");
                    m.setPlaca("ELIMINADA");
                }
                clientMotos.add(m);
            }
        }

        if (representativePlan == null) {
            return "redirect:/clients?error=Cliente+no+encontrado";
        }

        model.addAttribute("buyer", representativePlan.getBuyer());
        model.addAttribute("plans", clientPlans);
        model.addAttribute("motos", clientMotos);

        double totalDeuda = clientPlans.stream().mapToDouble(FinancingPlan::getSaldoPendiente).sum();
        double totalPagado = clientPlans.stream().mapToDouble(FinancingPlan::getTotalPagado).sum();
        model.addAttribute("totalDeuda", totalDeuda);
        model.addAttribute("totalPagado", totalPagado);

        return "clients/detail";
    }
}
