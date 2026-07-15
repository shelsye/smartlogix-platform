package com.smartlogix.order.client;

public record UpdateShipmentRequest(
        String destinationAddress,
        String region,
        String routeType,
        int totalUnits
) {}
