package com.moto.config;

import com.moto.model.User;
import com.moto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Seed or Reset default Admin
        User admin = userRepository.findByUsername("cristian").orElse(new User());
        admin.setUsername("cristian");
        admin.setPassword(passwordEncoder.encode("barraza1998"));
        admin.setFullName("Administrador del Concesionario");
        admin.setRole("ROLE_ADMIN");
        admin.setActive(true);
        userRepository.save(admin);
        System.out.println("Upserted default admin: cristian / barraza1998");

        // Seed or Reset default Employee
        User employee = userRepository.findByUsername("empleado").orElse(new User());
        employee.setUsername("empleado");
        employee.setPassword(passwordEncoder.encode("empleado123"));
        employee.setFullName("Empleado Concesionario");
        employee.setRole("ROLE_EMPLOYEE");
        employee.setActive(true);
        userRepository.save(employee);
        System.out.println("Upserted default employee: empleado / empleado123");
    }
}
