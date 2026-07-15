package com.smartlogix.order.dto;

public record CouponStatusResponse(
        String code,
        int discountPercent,
        boolean available,
        String message
) {}
