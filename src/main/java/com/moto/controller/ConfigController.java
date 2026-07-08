package com.moto.controller;

import com.moto.model.SystemConfig;
import com.moto.repository.SystemConfigRepository;
import com.moto.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private AuditService auditService;

    private String getCurrentTenantId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.moto.service.CustomUserDetails) {
            return ((com.moto.service.CustomUserDetails) auth.getPrincipal()).getTenantId();
        }
        return "default";
    }

    private String getConfigId(String tenantId) {
        if ("default".equals(tenantId)) {
            return "global_config";
        }
        return tenantId + "_config";
    }

    @GetMapping
    public String showConfig(Model model) {
        String tenantId = getCurrentTenantId();
        String configId = getConfigId(tenantId);

        SystemConfig config = systemConfigRepository.findById(configId)
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setId(configId);
                    newConfig.setTenantId(tenantId);
                    if (!"default".equals(tenantId)) {
                        newConfig.setCompanyName("Mi Negocio");
                    }
                    return systemConfigRepository.save(newConfig);
                });

        model.addAttribute("config", config);
        return "config";
    }

    @PostMapping("/save")
    public String saveConfig(@ModelAttribute("config") SystemConfig formConfig, HttpServletRequest request) {
        String tenantId = getCurrentTenantId();
        String configId = getConfigId(tenantId);

        SystemConfig dbConfig = systemConfigRepository.findById(configId)
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setId(configId);
                    newConfig.setTenantId(tenantId);
                    return newConfig;
                });

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
        
        // Only super admin can change global maintenance mode or banner
        boolean isSuperAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (isSuperAdmin && "default".equals(tenantId)) {
            dbConfig.setAnnouncementBanner(formConfig.getAnnouncementBanner());
            dbConfig.setMaintenanceMode(formConfig.isMaintenanceMode());
        }

        systemConfigRepository.save(dbConfig);
        auditService.log("MODIFICAR_CONFIGURACION", "Configuración de negocio modificada");
        return "redirect:/config?success=Configuracion+guardada+correctamente";
    }
}
