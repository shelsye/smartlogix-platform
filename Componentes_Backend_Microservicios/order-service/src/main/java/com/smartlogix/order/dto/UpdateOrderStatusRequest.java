package com.smartlogix.order.dto;

import com.smartlogix.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "El estado de la orden es obligatorio")
        OrderStatus status,

        String trackingCode,

        String reason
) {
}