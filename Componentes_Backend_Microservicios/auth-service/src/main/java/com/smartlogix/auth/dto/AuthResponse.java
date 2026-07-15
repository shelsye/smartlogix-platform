package com.smartlogix.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String firstName,
        String lastName,
        String username,
        String email,
        String role,
        long expiresInMs
) {}
