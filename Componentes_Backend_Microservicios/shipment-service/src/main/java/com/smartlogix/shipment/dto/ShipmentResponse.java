package com.smartlogix.shipment.dto;

import com.smartlogix.shipment.domain.ShipmentStatus;
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
        ShipmentStatus status,
        OffsetDateTime createdAt
) {}
