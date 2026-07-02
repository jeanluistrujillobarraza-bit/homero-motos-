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

    public void log(String action, String details) {
        String username = "SYSTEM";
        Object principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .getUsername();
        } else if (principal != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String) {
            username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }

        AuditLog log = new AuditLog(username, action, details, LocalDateTime.now(java.time.ZoneId.of("America/Bogota")));
        auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findFirst10ByOrderByFechaDesc();
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findByOrderByFechaDesc();
    }
}
