package com.smartlogix.auth.strategy;

import com.smartlogix.auth.domain.UserEntity;
import com.smartlogix.auth.repository.UserRepository;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class LocalUsernameAuthStrategy implements AuthenticationStrategy {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public LocalUsernameAuthStrategy(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(String credential) {
        return credential != null && !credential.contains("@");
    }

    @Override
    public UserEntity authenticate(String credential, String password) {
        UserEntity user = repository.findByUsernameIgnoreCase(credential.trim())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas."));
        if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas.");
        }
        return user;
    }
}
