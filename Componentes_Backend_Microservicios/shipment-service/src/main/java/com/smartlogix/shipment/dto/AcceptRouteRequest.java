package com.smartlogix.shipment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AcceptRouteRequest(
        @NotBlank String region,
        @Min(1) int totalUnits,
        @NotBlank String routeType
) {
}
