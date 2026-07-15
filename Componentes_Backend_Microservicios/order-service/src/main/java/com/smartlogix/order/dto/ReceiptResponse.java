package com.smartlogix.order.dto;

import com.smartlogix.order.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ReceiptResponse(
        String receiptNumber,
        String orderNumber,
        OffsetDateTime issuedAt,
        String customerName,
        String customerEmail,
        String shippingAddress,
        String shippingRegion,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String paymentReference,
        String authorizationCode,
        String cardBrand,
        String maskedCard,
        int installments,
        PaymentStatus paymentStatus,
        String verificationCode,
        boolean voided,
        List<ReceiptLineResponse> lines
) {}
