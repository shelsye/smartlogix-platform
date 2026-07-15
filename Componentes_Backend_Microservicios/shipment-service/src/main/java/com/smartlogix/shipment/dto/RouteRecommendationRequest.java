package com.smartlogix.shipment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RouteRecommendationRequest(
        @NotBlank String region,
        @Min(1) int units
) {}
