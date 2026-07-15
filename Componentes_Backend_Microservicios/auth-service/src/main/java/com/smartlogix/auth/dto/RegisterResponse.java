package com.smartlogix.auth.dto;

public record RegisterResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        String welcomeCoupon,
        String message
) {}
