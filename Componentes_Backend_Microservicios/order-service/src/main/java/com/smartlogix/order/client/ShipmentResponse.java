package com.smartlogix.order.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ShipmentResponse(
        String trackingCode,
        String orderNumber,
        String destinationAddress,
        String region,
        String routeType,
        String routeName,
        String carrier,
        String routeCode,
        BigDecimal price,
        int estimatedDays,
        int distanceKm,
        LocalDate estimatedDeliveryDate,
        String status,
        OffsetDateTime createdAt
) {}
