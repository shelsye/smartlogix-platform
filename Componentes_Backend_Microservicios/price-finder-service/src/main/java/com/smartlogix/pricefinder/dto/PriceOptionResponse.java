package com.smartlogix.pricefinder.dto;

import java.math.BigDecimal;

public record PriceOptionResponse(
        String store,
        String seller,
        String title,
        BigDecimal price,
        String currency,
        String condition,
        String externalUrl,
        String imageUrl,
        String delivery,
        String source
) {}
