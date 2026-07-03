package com.moto.config;

import com.moto.model.SystemConfig;
import com.moto.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        SystemConfig config = systemConfigRepository.findById("global_config").orElse(null);
        if (config != null && config.isMaintenanceMode()) {
            String uri = request.getRequestURI();

            // Allow static assets, login, logout, maintenance, and super admin console
            if (uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images") || uri.startsWith("/webjars") 
                    || uri.equals("/maintenance") || uri.startsWith("/superadmin") || uri.equals("/login") || uri.equals("/logout")) {
                return true;
            }

            // Bypass for SUPER_ADMIN role
            Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (principal != null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()))) {
                return true;
            }

            response.sendRedirect("/maintenance");
            return false;
        }
        return true;
    }
}
