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

    private String getCurrentTenantId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.moto.service.CustomUserDetails) {
            return ((com.moto.service.CustomUserDetails) auth.getPrincipal()).getTenantId();
        }
        return "default";
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
            
            Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                user.setCreatedBy(((org.springframework.security.core.userdetails.UserDetails) principal).getUsername());
            } else {
                user.setCreatedBy("system");
            }

            if ("ROLE_BUSINESS_ADMIN".equals(user.getRole())) {
                user.setTenantId("tenant_" + java.util.UUID.randomUUID().toString());
            } else {
                user.setTenantId(getCurrentTenantId());
            }
            
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

    private boolean isCreatedByAdmin(User user) {
        String createdBy = user.getCreatedBy();
        if (createdBy == null) {
            return "cristian".equals(user.getUsername()) || "admin".equals(user.getUsername()) 
                || "ROLE_SUPER_ADMIN".equals(user.getRole()) || "ROLE_ADMIN".equals(user.getRole());
        }
        if ("cristian".equals(createdBy) || "admin".equals(createdBy) || "system".equals(createdBy)) {
            return true;
        }
        Optional<User> creatorOpt = userRepository.findByUsername(createdBy);
        if (creatorOpt.isPresent()) {
            User creator = creatorOpt.get();
            return "ROLE_SUPER_ADMIN".equals(creator.getRole()) || "ROLE_ADMIN".equals(creator.getRole());
        }
        return false;
    }

    @GetMapping("/toggle/{id}")
    public String toggleUserStatus(@PathVariable("id") String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Don't disable default admin to prevent self-lockout
            if ("admin".equals(user.getUsername()) || "cristian".equals(user.getUsername())) {
                return "redirect:/users?error=No+se+puede+desactivar+al+administrador+principal";
            }
            user.setActive(!user.isActive());
            userRepository.save(user);
            auditService.log("ESTADO_USUARIO", "Toggled estado del usuario: " + user.getUsername() + " a activo=" + user.isActive());
        }
        return "redirect:/users";
    }

    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if ("cristian".equals(user.getUsername()) || "admin".equals(user.getUsername())) {
                return "redirect:/users?error=No+se+puede+eliminar+al+administrador+principal";
            }
            
            // Prevent self-deletion
            Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String currentUsername = null;
            String currentRole = null;
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                currentUsername = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                if (currentUsername.equals(user.getUsername())) {
                    return "redirect:/users?error=No+se+puede+eliminar+a+si+mismo";
                }
            }
            if (principal instanceof com.moto.service.CustomUserDetails) {
                currentRole = ((com.moto.service.CustomUserDetails) principal).getUser().getRole();
            }
            
            // Check if current user is ROLE_BUSINESS_ADMIN and target user was created by an admin
            if ("ROLE_BUSINESS_ADMIN".equals(currentRole) && isCreatedByAdmin(user)) {
                return "redirect:/users?error=No+tiene+permisos+para+eliminar+usuarios+creados+por+el+administrador";
            }
            
            userRepository.delete(user);
            auditService.log("ELIMINACION_USUARIO", "Eliminado usuario: " + user.getUsername());
        }
        return "redirect:/users";
    }
}
