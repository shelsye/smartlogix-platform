package com.smartlogix.inventory.dto;

import jakarta.validation.constraints.Min;

public record StockUpdateRequest(@Min(0) int availableQuantity) {}
