package com.smartlogix.pricefinder.dto;

import java.util.List;

public record PriceSearchResponse(
        String query,
        int totalResults,
        boolean realPurchase,
        String paymentMode,
        String sourceMode,
        String message,
        PriceOptionResponse bestOption,
        List<PriceOptionResponse> results
) {}
