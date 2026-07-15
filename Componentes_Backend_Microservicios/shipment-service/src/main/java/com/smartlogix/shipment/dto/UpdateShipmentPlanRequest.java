package com.smartlogix.shipment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateShipmentPlanRequest(
        @NotBlank String destinationAddress,
        @NotBlank String region,
        @NotBlank String routeType,
        @Min(1) int totalUnits
) {}
