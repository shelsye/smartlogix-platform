package com.smartlogix.order.client;

import java.math.BigDecimal;

public record ProductSnapshotResponse(
        String sku,
        String productName,
        BigDecimal price,
        int requestedQuantity,
        int availableQuantity,
        boolean active,
        boolean available
) {}
