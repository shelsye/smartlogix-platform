package com.smartlogix.auth.config;

import com.smartlogix.auth.domain.Role;
import com.smartlogix.auth.domain.UserEntity;
import com.smartlogix.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;

@Configuration
public class DataSeedConfig {
    @Bean
    @Order(1)
    CommandLineRunner ensureAdministrator(
            UserRepository repository,
            PasswordEncoder encoder,
            @Value("${smartlogix.admin.email:admin@smartlogix.com}") String email,
            @Value("${smartlogix.admin.password:admin1}") String password,
            @Value("${smartlogix.admin.first-name:Administrador}") String firstName,
            @Value("${smartlogix.admin.last-name:SmartLogix}") String lastName) {
        return args -> {
            String normalizedEmail = email.trim().toLowerCase();
            UserEntity admin = repository.findByEmailIgnoreCase(normalizedEmail).orElseGet(UserEntity::new);
            admin.setFirstName(firstName.trim());
            admin.setLastName(lastName.trim());
            admin.setEmail(normalizedEmail);
            admin.setUsername(normalizedEmail);
            admin.setPassword(encoder.encode(password));
            admin.setRole(Role.ROLE_ADMIN);
            admin.setEnabled(true);
            repository.save(admin);
        };
    }
}
