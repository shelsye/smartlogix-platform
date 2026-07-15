package com.smartlogix.auth.dto;

import java.time.OffsetDateTime;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String username,
        String email,
        String role,
        boolean enabled,
        OffsetDateTime createdAt
) {}
