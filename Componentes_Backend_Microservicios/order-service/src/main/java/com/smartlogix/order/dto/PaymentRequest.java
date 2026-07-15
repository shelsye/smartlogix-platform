package com.smartlogix.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentRequest(
        @NotBlank String cardHolderName,
        @NotBlank @Pattern(regexp = "[0-9 ]{13,23}", message = "El número de tarjeta es inválido") String cardNumber,
        @Min(1) @Max(12) int expiryMonth,
        @Min(2020) int expiryYear,
        @NotBlank @Pattern(regexp = "[0-9]{3,4}", message = "El CVV es inválido") String securityCode,
        @Min(1) @Max(12) int installments
) {}
