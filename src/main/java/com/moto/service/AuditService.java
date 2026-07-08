package com.moto.service;

import com.moto.model.AuditLog;
import com.moto.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String getCurrentTenantId() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getTenantId();
            }
        }
        return "default";
    }

    public void log(String action, String details) {
        String username = "SYSTEM";
        String tenantId = "default";
        Object principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .getUsername();
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CustomUserDetails) {
                tenantId = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getTenantId();
            }
        } else if (principal != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String) {
            username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }

        AuditLog log = new AuditLog(username, action, details, LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        log.setTenantId(tenantId);
        auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findFirst10ByTenantIdOrderByFechaDesc(getCurrentTenantId());
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findByTenantIdOrderByFechaDesc(getCurrentTenantId());
    }
}
