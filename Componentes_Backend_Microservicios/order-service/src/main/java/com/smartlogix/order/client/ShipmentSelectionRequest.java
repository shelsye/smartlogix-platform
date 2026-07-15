package com.smartlogix.order.client;

public record ShipmentSelectionRequest(
        String selectionId,
        Long userId,
        String orderNumber,
        String destinationAddress,
        int totalUnits
) {
}
