package com.smartlogix.shipment.security;

public record AuthenticatedUser(Long userId, String email) {
}
