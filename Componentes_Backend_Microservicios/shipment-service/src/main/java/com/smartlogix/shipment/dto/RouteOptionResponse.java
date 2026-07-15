package com.smartlogix.shipment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RouteOptionResponse(
        String type,
        String routeName,
        String carrier,
        BigDecimal price,
        int estimatedDays,
        int distanceKm,
        LocalDate estimatedDate,
        double score
) {}
