package com.smartlogix.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateInventoryItemRequest(
        @NotBlank @Size(max = 120) String productName,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 40) String warehouseCode,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank @Size(max = 1000) String description,
        @Size(max = 1000) String imageUrl,
        @Min(0) int reorderLevel,
        boolean active
) {}
