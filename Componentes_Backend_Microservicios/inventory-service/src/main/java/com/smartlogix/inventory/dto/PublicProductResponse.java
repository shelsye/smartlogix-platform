package com.smartlogix.inventory.dto;

import java.math.BigDecimal;

public record PublicProductResponse(
        String sku,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        boolean available
) {}
