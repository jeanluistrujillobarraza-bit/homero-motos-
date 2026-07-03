package com.moto.controller;

import com.moto.model.*;
import com.moto.repository.*;
import com.moto.service.AuditService;
import com.moto.service.FinancingService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/superadmin")
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private BackupRecordRepository backupRecordRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private FinancingService financingService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String index(@RequestParam(value = "tab", defaultValue = "dashboard") String tab,
                        @RequestParam(value = "success", required = false) String success,
                        @RequestParam(value = "error", required = false) String error,
                        Model model) {
        
        SystemConfig config = systemConfigRepository.findById("global_config")
                .orElseGet(() -> {
                    SystemConfig defaultConf = new SystemConfig();
                    return systemConfigRepository.save(defaultConf);
                });

        model.addAttribute("tab", tab);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("config", config);

        // Load data based on tab
        if ("dashboard".equals(tab)) {
            // General Stats
            List<Motorcycle> allMotos = motorcycleRepository.findAll();
            model.addAttribute("totalMotos", allMotos.stream().filter(m -> !m.isDeleted()).count());
            model.addAttribute("motosDestacadas", allMotos.stream().filter(m -> m.isDestacado() && !m.isDeleted()).count());
            model.addAttribute("motosOcultas", allMotos.stream().filter(m -> m.isHidden() && !m.isDeleted()).count());

            List<FinancingPlan> allPlans = financingPlanRepository.findAll();
            double totalRevenue = paymentRepository.findAll().stream().mapToDouble(Payment::getValorPagado).sum();
            double pendingRevenue = allPlans.stream().mapToDouble(FinancingPlan::getSaldoPendiente).sum();
            long overduePlans = allPlans.stream().filter(p -> "ATRASADO".equals(p.getEstadoCredito())).count();

            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("pendingRevenue", pendingRevenue);
            model.addAttribute("overduePlans", overduePlans);
            model.addAttribute("totalUsers", userRepository.count());
            model.addAttribute("recentLogs", auditLogRepository.findFirst10ByOrderByFechaDesc());
        } else if ("users".equals(tab)) {
            model.addAttribute("users", userRepository.findAll());
        } else if ("backups".equals(tab)) {
            model.addAttribute("backups", backupRecordRepository.findByOrderByCreatedAtDesc());
        } else if ("trash".equals(tab)) {
            model.addAttribute("deletedMotos", motorcycleRepository.findAll().stream().filter(Motorcycle::isDeleted).collect(Collectors.toList()));
        } else if ("audit".equals(tab)) {
            model.addAttribute("logs", auditLogRepository.findAll());
        }

        return "superadmin/dashboard";
    }

    // --- SYSTEM CONFIG ---
    @PostMapping("/config/save")
    public String saveConfig(@ModelAttribute("config") SystemConfig formConfig, HttpServletRequest request) {
        SystemConfig dbConfig = systemConfigRepository.findById("global_config").orElse(new SystemConfig());
        dbConfig.setCompanyName(formConfig.getCompanyName());
        dbConfig.setContactPhone(formConfig.getContactPhone());
        dbConfig.setAddress(formConfig.getAddress());
        dbConfig.setEmailConfigHost(formConfig.getEmailConfigHost());
        dbConfig.setEmailConfigPort(formConfig.getEmailConfigPort());
        dbConfig.setEmailConfigUser(formConfig.getEmailConfigUser());
        dbConfig.setEmailConfigPassword(formConfig.getEmailConfigPassword());
        dbConfig.setWhatsappToken(formConfig.getWhatsappToken());
        dbConfig.setColorPrimary(formConfig.getColorPrimary());
        dbConfig.setColorSecondary(formConfig.getColorSecondary());
        dbConfig.setMaxFinancingTimeMonths(formConfig.getMaxFinancingTimeMonths());
        dbConfig.setDefaultInterestRate(formConfig.getDefaultInterestRate());
        dbConfig.setAnnouncementBanner(formConfig.getAnnouncementBanner());
        dbConfig.setMaintenanceMode(formConfig.isMaintenanceMode());
        
        systemConfigRepository.save(dbConfig);
        logAuditWithDetails("MODIFICAR_CONFIGURACION", "Configuración global modificada", request);
        return "redirect:/superadmin?tab=system&success=Configuracion+guardada";
    }

    // --- USER MANAGEMENT ---
    @PostMapping("/users/role")
    public String updateUserRole(@RequestParam("userId") String userId, @RequestParam("role") String role, HttpServletRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setRole(role);
        userRepository.save(user);
        logAuditWithDetails("CAMBIAR_ROL", "Rol del usuario " + user.getUsername() + " cambiado a: " + role, request);
        return "redirect:/superadmin?tab=users&success=Rol+actualizado+exitosamente";
    }

    @PostMapping("/users/status")
    public String updateUserStatus(@RequestParam("userId") String userId, @RequestParam("suspended") boolean suspended, HttpServletRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setSuspended(suspended);
        user.setActive(!suspended);
        userRepository.save(user);
        String action = suspended ? "SUSPENDER_USUARIO" : "ACTIVAR_USUARIO";
        logAuditWithDetails(action, "Estado del usuario " + user.getUsername() + " cambiado. Suspendido: " + suspended, request);
        return "redirect:/superadmin?tab=users&success=Estado+de+usuario+actualizado";
    }

    @PostMapping("/users/reset-password")
    public String resetUserPassword(@RequestParam("userId") String userId, @RequestParam("newPassword") String newPassword, HttpServletRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logAuditWithDetails("RESTABLECER_CONTRASENA", "Contraseña restablecida para el usuario " + user.getUsername(), request);
        return "redirect:/superadmin?tab=users&success=Contrasena+restablecida+correctamente";
    }

    // --- BACKUP & RESTORE ---
    @GetMapping("/backups/create")
    public String createBackup(HttpServletRequest request) {
        try {
            List<Motorcycle> motos = motorcycleRepository.findAll();
            List<User> users = userRepository.findAll();
            List<FinancingPlan> plans = financingPlanRepository.findAll();
            List<Payment> payments = paymentRepository.findAll();
            List<SystemConfig> configs = systemConfigRepository.findAll();
            List<AuditLog> logs = auditLogRepository.findAll();

            Map<String, Object> backupData = new HashMap<>();
            backupData.put("motorcycles", motos);
            backupData.put("users", users);
            backupData.put("financing_plans", plans);
            backupData.put("payments", payments);
            backupData.put("system_config", configs);
            backupData.put("audit_logs", logs);

            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(backupData);

            String filename = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".json";

            BackupRecord record = new BackupRecord();
            record.setFilename(filename);
            record.setCreatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
            record.setSizeBytes((long) json.getBytes().length);
            record.setStatus("SUCCESS");
            backupRecordRepository.save(record);

            File dir = new File("backups");
            if (!dir.exists()) dir.mkdirs();
            Files.writeString(Path.of("backups", filename), json);

            logAuditWithDetails("CREAR_BACKUP", "Copia de seguridad creada: " + filename, request);
            return "redirect:/superadmin?tab=backups&success=Copia+de+seguridad+creada";
        } catch (Exception e) {
            log.error("Error creating backup", e);
            return "redirect:/superadmin?tab=backups&error=Error+al+crear+copia+de+seguridad";
        }
    }

    @PostMapping("/backups/restore")
    public String restoreBackup(@RequestParam("backupFile") MultipartFile file, HttpServletRequest request) {
        try {
            byte[] bytes = file.getBytes();
            String json = new String(bytes);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(json, Map.class);

            if (data.containsKey("users")) {
                userRepository.deleteAll();
                List<User> list = mapper.convertValue(data.get("users"), new com.fasterxml.jackson.core.type.TypeReference<List<User>>() {});
                userRepository.saveAll(list);
            }
            if (data.containsKey("motorcycles")) {
                motorcycleRepository.deleteAll();
                List<Motorcycle> list = mapper.convertValue(data.get("motorcycles"), new com.fasterxml.jackson.core.type.TypeReference<List<Motorcycle>>() {});
                motorcycleRepository.saveAll(list);
            }
            if (data.containsKey("financing_plans")) {
                financingPlanRepository.deleteAll();
                List<FinancingPlan> list = mapper.convertValue(data.get("financing_plans"), new com.fasterxml.jackson.core.type.TypeReference<List<FinancingPlan>>() {});
                financingPlanRepository.saveAll(list);
            }
            if (data.containsKey("payments")) {
                paymentRepository.deleteAll();
                List<Payment> list = mapper.convertValue(data.get("payments"), new com.fasterxml.jackson.core.type.TypeReference<List<Payment>>() {});
                paymentRepository.saveAll(list);
            }
            if (data.containsKey("system_config")) {
                systemConfigRepository.deleteAll();
                List<SystemConfig> list = mapper.convertValue(data.get("system_config"), new com.fasterxml.jackson.core.type.TypeReference<List<SystemConfig>>() {});
                systemConfigRepository.saveAll(list);
            }
            if (data.containsKey("audit_logs")) {
                auditLogRepository.deleteAll();
                List<AuditLog> list = mapper.convertValue(data.get("audit_logs"), new com.fasterxml.jackson.core.type.TypeReference<List<AuditLog>>() {});
                auditLogRepository.saveAll(list);
            }

            logAuditWithDetails("RESTAURAR_BACKUP", "Base de datos restaurada desde archivo subido", request);
            return "redirect:/superadmin?tab=backups&success=Base+de+datos+restaurada+correctamente";
        } catch (Exception e) {
            log.error("Error restoring backup", e);
            return "redirect:/superadmin?tab=backups&error=Error+al+restaurar+la+base+de+datos";
        }
    }

    @GetMapping("/backups/download/{filename}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadBackup(@PathVariable("filename") String filename, HttpServletRequest request) {
        try {
            Path filePath = Path.of("backups", filename);
            if (!Files.exists(filePath)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            byte[] fileBytes = Files.readAllBytes(filePath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            logAuditWithDetails("DESCARGAR_BACKUP", "Copia de seguridad descargada: " + filename, request);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error downloading backup file", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // --- TRASH CAN (GARBAGE BIN) ---
    @GetMapping("/trash/restore/{id}")
    public String restoreMotorcycle(@PathVariable("id") String id, HttpServletRequest request) {
        Motorcycle m = motorcycleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Motocicleta no encontrada"));
        m.setDeleted(false);
        motorcycleRepository.save(m);
        logAuditWithDetails("RESTAURAR_MOTOCICLETA", "Motocicleta restaurada: " + m.getPlaca(), request);
        return "redirect:/superadmin?tab=trash&success=Motocicleta+restaurada";
    }

    // --- UTILS ---
    private void logAuditWithDetails(String action, String details, HttpServletRequest request) {
        String username = "SYSTEM";
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (principal != null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        String userAgent = request.getHeader("User-Agent");
        AuditLog log = new AuditLog(username, action, details, LocalDateTime.now(ZoneId.of("America/Bogota")), ip, userAgent);
        auditLogRepository.save(log);
    }
}
