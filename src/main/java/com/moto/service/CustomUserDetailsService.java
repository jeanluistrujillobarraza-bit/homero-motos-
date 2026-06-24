package com.moto.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.moto.model.User;
import com.moto.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Intentando autenticar al usuario: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado en MongoDB: {}", username);
                    return new UsernameNotFoundException("Usuario no encontrado: " + username);
                });
        
        log.debug("Usuario encontrado: {}, Activo: {}, Rol: {}", user.getUsername(), user.isActive(), user.getRole());
        
        if (!user.isActive()) {
            log.warn("El usuario está inactivo: {}", username);
            throw new UsernameNotFoundException("Usuario inactivo: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
