package com.moto.controller;

import com.moto.model.Motorcycle;
import com.moto.model.FinancingPlan;
import com.moto.model.Payment;
import com.moto.repository.MotorcycleRepository;
import com.moto.repository.FinancingPlanRepository;
import com.moto.repository.PaymentRepository;
import com.moto.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/motorcycles")
public class MotorcycleController {

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditService auditService;

    @GetMapping
    public String listMotorcycles(Model model) {
        java.util.List<Motorcycle> list = motorcycleRepository.findAll().stream()
                .filter(m -> !m.isDeleted())
                .collect(Collectors.toList());
        model.addAttribute("motorcycles", list);
        return "motorcycles/list";
    }

    @GetMapping("/new")
    public String newMotorcycleForm(Model model) {
        model.addAttribute("motorcycle", new Motorcycle());
        return "motorcycles/form";
    }

    @PostMapping("/save")
    public String saveMotorcycle(@ModelAttribute("motorcycle") Motorcycle motorcycle,
                                 @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                 Model model) {
        if (motorcycle.getId() == null || motorcycle.getId().trim().isEmpty()) {
            motorcycle.setId(null);
            // New motorcycle checks
            if (motorcycleRepository.findByPlaca(motorcycle.getPlaca()).isPresent()) {
                model.addAttribute("error", "La placa ya está registrada en el sistema.");
                return "motorcycles/form";
            }
            if (motorcycleRepository.findByVin(motorcycle.getVin()).isPresent()) {
                model.addAttribute("error", "El VIN ya está registrado en el sistema.");
                return "motorcycles/form";
            }
            if (motorcycleRepository.findByNumeroMotor(motorcycle.getNumeroMotor()).isPresent()) {
                model.addAttribute("error", "El número de motor ya está registrado en el sistema.");
                return "motorcycles/form";
            }
            motorcycle.setEstado("DISPONIBLE");
        } else {
            // Edit check: check duplicates excluding current id
            Optional<Motorcycle> existingPlaca = motorcycleRepository.findByPlaca(motorcycle.getPlaca());
            if (existingPlaca.isPresent() && !existingPlaca.get().getId().equals(motorcycle.getId())) {
                model.addAttribute("error", "La placa ya pertenece a otra motocicleta.");
                return "motorcycles/form";
            }
            Optional<Motorcycle> existingVin = motorcycleRepository.findByVin(motorcycle.getVin());
            if (existingVin.isPresent() && !existingVin.get().getId().equals(motorcycle.getId())) {
                model.addAttribute("error", "El VIN ya pertenece a otra motocicleta.");
                return "motorcycles/form";
            }
            Optional<Motorcycle> existingMotor = motorcycleRepository.findByNumeroMotor(motorcycle.getNumeroMotor());
            if (existingMotor.isPresent() && !existingMotor.get().getId().equals(motorcycle.getId())) {
                model.addAttribute("error", "El número de motor ya pertenece a otra motocicleta.");
                return "motorcycles/form";
            }
            
            // Retain original image if no new image is provided
            Motorcycle original = motorcycleRepository.findById(motorcycle.getId()).orElse(null);
            if (original != null) {
                if (imageFile == null || imageFile.isEmpty()) {
                    motorcycle.setFotoBase64(original.getFotoBase64());
                }
                if (motorcycle.getEstado() == null) {
                    motorcycle.setEstado(original.getEstado());
                }
                motorcycle.setDeleted(original.isDeleted());
                motorcycle.setDestacado(original.isDestacado());
                motorcycle.setHidden(original.isHidden());
            }
        }

        // Handle image conversion
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String base64 = Base64.getEncoder().encodeToString(imageFile.getBytes());
                String mimeType = imageFile.getContentType();
                motorcycle.setFotoBase64("data:" + mimeType + ";base64," + base64);
            } catch (IOException e) {
                model.addAttribute("error", "Error al procesar la imagen de la motocicleta.");
                return "motorcycles/form";
            }
        }

        motorcycleRepository.save(motorcycle);
        auditService.log("GUARDAR_MOTOCICLETA", "Guardada motocicleta con placa: " + motorcycle.getPlaca());
        return "redirect:/motorcycles";
    }

    @GetMapping("/edit/{id}")
    public String editMotorcycleForm(@PathVariable("id") String id, Model model) {
        Motorcycle motorcycle = motorcycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Motocicleta no encontrada"));
        model.addAttribute("motorcycle", motorcycle);
        return "motorcycles/form";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String deleteMotorcycle(@PathVariable("id") String id, Model model) {
        Motorcycle motorcycle = motorcycleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Motocicleta no encontrada"));
        
        if ("EN_FINANCIACION".equals(motorcycle.getEstado())) {
            // Cannot delete active financing
            return "redirect:/motorcycles?error=No+se+puede+eliminar+una+moto+bajo+financiacion+activa";
        }

        motorcycleRepository.deleteById(id);
        auditService.log("ELIMINAR_MOTOCICLETA", "Eliminada motocicleta con placa: " + motorcycle.getPlaca());
        return "redirect:/motorcycles";
    }
}
