package com.smartlogix.order.client;

public record ShipmentRequest(
        String orderNumber,
        String destinationAddress,
        String region,
        String routeType,
        int totalUnits
) {}
