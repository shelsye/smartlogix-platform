package com.smartlogix.auth.controller;

import com.smartlogix.auth.dto.*;
import com.smartlogix.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final String internalApiKey;

    public AuthController(AuthService service,
                          @Value("${smartlogix.internal-api-key}") String internalApiKey) {
        this.service = service;
        this.internalApiKey = internalApiKey;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/validate")
    public AuthResponse validate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) String token) {
        return service.validateToken(resolveToken(authorization, token));
    }

    @GetMapping("/me")
    public UserDTO me(Authentication authentication) {
        return service.getCurrentUser(authentication.getName());
    }

    @PutMapping("/me")
    public UserDTO updateMe(Authentication authentication,
                            @Valid @RequestBody ProfileUpdateRequest request) {
        return service.updateCurrentUser(authentication.getName(), request);
    }

    @GetMapping("/users")
    public List<UserDTO> users() {
        return service.getAllUsers();
    }

    @GetMapping("/statistics/count")
    public UserCountResponse count() {
        return service.countUsers();
    }

    @PutMapping("/users/{id}")
    public UserDTO updateUser(@PathVariable Long id,
                              @Valid @RequestBody UpdateUserRequest request) {
        return service.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        service.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/users/{userId}/coupons/{code}")
    public UserCouponResponse internalCoupon(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable Long userId,
            @PathVariable String code) {
        requireInternalKey(key);
        return service.couponStatus(userId, code);
    }

    @PostMapping("/internal/users/{userId}/coupons/{code}/consume")
    public UserCouponResponse consumeCoupon(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable Long userId,
            @PathVariable String code,
            @RequestParam String orderNumber) {
        requireInternalKey(key);
        return service.consumeCoupon(userId, code, orderNumber);
    }

    @PostMapping("/internal/users/{userId}/coupons/{code}/release")
    public UserCouponResponse releaseCoupon(
            @RequestHeader("X-Internal-Api-Key") String key,
            @PathVariable Long userId,
            @PathVariable String code,
            @RequestParam String orderNumber) {
        requireInternalKey(key);
        return service.releaseCoupon(userId, code, orderNumber);
    }

    private void requireInternalKey(String key) {
        if (!internalApiKey.equals(key)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Clave interna inválida.");
        }
    }

    private String resolveToken(String authorization, String token) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return token;
    }
}
