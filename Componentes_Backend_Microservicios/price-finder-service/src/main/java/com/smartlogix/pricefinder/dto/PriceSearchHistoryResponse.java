package com.smartlogix.pricefinder.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PriceSearchHistoryResponse(
        Long id,
        String query,
        String bestStore,
        String bestProduct,
        BigDecimal bestPrice,
        String currency,
        int resultsCount,
        String sourceMode,
        OffsetDateTime searchedAt
) {}
