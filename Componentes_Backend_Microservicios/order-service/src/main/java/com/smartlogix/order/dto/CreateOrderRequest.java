package com.smartlogix.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String customerName,
        @NotBlank String shippingAddress,
        @NotBlank String shippingRegion,
        @NotBlank String shippingType,
        @NotBlank String routeSelectionId,
        @NotEmpty List<@Valid OrderLineRequest> lines,
        String couponCode,
        @NotNull @Valid PaymentRequest payment
) {}
