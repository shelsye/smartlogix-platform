package com.smartlogix.shipment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateShipmentFromSelectionRequest(
        @NotBlank String selectionId,
        @NotNull Long userId,
        @NotBlank String orderNumber,
        @NotBlank String destinationAddress,
        @Min(1) int totalUnits
) {
}
