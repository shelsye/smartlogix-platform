package com.smartlogix.shipment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AcceptedRouteResponse(
        String selectionId,
        String region,
        int totalUnits,
        String type,
        String routeName,
        String carrier,
        String routeCode,
        BigDecimal price,
        int estimatedDays,
        int distanceKm,
        LocalDate estimatedDate,
        String orderNumber,
        OffsetDateTime acceptedAt
) {
}
