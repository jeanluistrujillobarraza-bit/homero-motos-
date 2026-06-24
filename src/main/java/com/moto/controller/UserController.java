package com.moto.controller;

import com.moto.model.User;
import com.moto.repository.UserRepository;
import com.moto.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("newUser", new User());
        return "users/list";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute("newUser") User user, Model model) {
        if (user.getId() == null || user.getId().trim().isEmpty()) {
            user.setId(null);
        }
        if (user.getId() == null) {
            // New user registration
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                model.addAttribute("error", "El nombre de usuario ya está en uso.");
                model.addAttribute("users", userRepository.findAll());
                return "users/list";
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            user.setActive(true);
            userRepository.save(user);
            auditService.log("CREACION_USUARIO", "Creado usuario interno: " + user.getUsername() + " con rol: " + user.getRole());
        } else {
            // Editing existing user
            Optional<User> existingOpt = userRepository.findById(user.getId());
            if (existingOpt.isPresent()) {
                User original = existingOpt.get();
                
                // Username change duplication check
                if (!original.getUsername().equals(user.getUsername())) {
                    if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                        model.addAttribute("error", "El nombre de usuario ya está en uso.");
                        model.addAttribute("users", userRepository.findAll());
                        return "users/list";
                    }
                }
                
                original.setUsername(user.getUsername());
                original.setFullName(user.getFullName());
                original.setRole(user.getRole());
                
                // If password was edited
                if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                    original.setPassword(passwordEncoder.encode(user.getPassword()));
                }
                
                userRepository.save(original);
                auditService.log("MODIFICACION_USUARIO", "Modificado usuario interno: " + original.getUsername());
            }
        }
        return "redirect:/users";
    }

    @GetMapping("/toggle/{id}")
    public String toggleUserStatus(@PathVariable("id") String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Don't disable default admin to prevent self-lockout
            if ("admin".equals(user.getUsername())) {
                return "redirect:/users?error=No+se+puede+desactivar+al+administrador+principal";
            }
            user.setActive(!user.isActive());
            userRepository.save(user);
            auditService.log("ESTADO_USUARIO", "Toggled estado del usuario: " + user.getUsername() + " a activo=" + user.isActive());
        }
        return "redirect:/users";
    }
}
