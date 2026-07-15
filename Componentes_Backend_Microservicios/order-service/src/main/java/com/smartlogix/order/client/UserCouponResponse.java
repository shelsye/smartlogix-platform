package com.smartlogix.order.client;

import java.time.OffsetDateTime;

public record UserCouponResponse(
        String code,
        int discountPercentage,
        boolean available,
        boolean used,
        OffsetDateTime issuedAt,
        OffsetDateTime usedAt,
        String orderNumber,
        String message
) {}
