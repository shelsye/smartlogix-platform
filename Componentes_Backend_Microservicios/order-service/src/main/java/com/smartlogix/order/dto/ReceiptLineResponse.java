package com.smartlogix.order.dto;

import java.math.BigDecimal;

public record ReceiptLineResponse(
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
