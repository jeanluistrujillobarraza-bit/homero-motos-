package com.moto.config;

import com.moto.model.*;
import com.moto.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MotorcycleRepository motorcycleRepository;

    @Autowired
    private FinancingPlanRepository financingPlanRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Seed or Reset default Admin
        User admin = userRepository.findByUsername("cristian").orElse(new User());
        admin.setUsername("cristian");
        admin.setPassword(passwordEncoder.encode("barraza1998"));
        admin.setFullName("Administrador del Concesionario");
        admin.setRole("ROLE_SUPER_ADMIN");
        admin.setActive(true);
        admin.setTenantId("default");
        userRepository.save(admin);
        System.out.println("Upserted default admin: cristian / barraza1998");

        // Seed or Reset default Employee
        User employee = userRepository.findByUsername("empleado").orElse(new User());
        employee.setUsername("empleado");
        employee.setPassword(passwordEncoder.encode("empleado123"));
        employee.setFullName("Empleado Concesionario");
        employee.setRole("ROLE_EMPLOYEE");
        employee.setActive(true);
        employee.setTenantId("default");
        userRepository.save(employee);
        System.out.println("Upserted default employee: empleado / empleado123");

        // Seed default system config if it doesn't exist
        if (!systemConfigRepository.findById("global_config").isPresent()) {
            SystemConfig defaultConfig = new SystemConfig();
            defaultConfig.setId("global_config");
            defaultConfig.setTenantId("default");
            systemConfigRepository.save(defaultConfig);
            System.out.println("Created default global system configuration.");
        }

        // Run data migration for existing records (multi-tenancy backward compatibility)
        System.out.println("Running data migration for null tenantIds...");
        
        long usersMigrated = 0;
        for (User u : userRepository.findAll()) {
            if (u.getTenantId() == null) {
                u.setTenantId("default");
                userRepository.save(u);
                usersMigrated++;
            }
        }
        
        long motorcyclesMigrated = 0;
        for (Motorcycle m : motorcycleRepository.findAll()) {
            if (m.getTenantId() == null) {
                m.setTenantId("default");
                motorcycleRepository.save(m);
                motorcyclesMigrated++;
            }
        }
        
        long plansMigrated = 0;
        for (FinancingPlan fp : financingPlanRepository.findAll()) {
            if (fp.getTenantId() == null) {
                fp.setTenantId("default");
                financingPlanRepository.save(fp);
                plansMigrated++;
            }
        }
        
        long paymentsMigrated = 0;
        for (Payment p : paymentRepository.findAll()) {
            if (p.getTenantId() == null) {
                p.setTenantId("default");
                paymentRepository.save(p);
                paymentsMigrated++;
            }
        }
        
        long logsMigrated = 0;
        for (AuditLog al : auditLogRepository.findAll()) {
            if (al.getTenantId() == null) {
                al.setTenantId("default");
                auditLogRepository.save(al);
                logsMigrated++;
            }
        }
        
        long configsMigrated = 0;
        for (SystemConfig sc : systemConfigRepository.findAll()) {
            if (sc.getTenantId() == null) {
                sc.setTenantId("default");
                systemConfigRepository.save(sc);
                configsMigrated++;
            }
        }
        
        System.out.println(String.format("Migration finished. Users: %d, Motorcycles: %d, Plans: %d, Payments: %d, Logs: %d, Configs: %d",
                usersMigrated, motorcyclesMigrated, plansMigrated, paymentsMigrated, logsMigrated, configsMigrated));
    }
}
