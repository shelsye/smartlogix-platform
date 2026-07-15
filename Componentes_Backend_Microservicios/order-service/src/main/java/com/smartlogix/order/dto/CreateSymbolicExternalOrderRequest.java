package com.smartlogix.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateSymbolicExternalOrderRequest(
        @NotBlank String customerName,
        @NotBlank String externalStore,
        String externalSeller,
        @NotBlank String externalProductName,
        @NotNull @DecimalMin(value = "1.0", message = "El precio externo debe ser mayor a 0") BigDecimal externalPrice,
        String currency,
        String externalUrl,
        String imageUrl,
        String shippingAddress,
        String shippingRegion,
        @NotNull @Valid PaymentRequest payment
) {}
