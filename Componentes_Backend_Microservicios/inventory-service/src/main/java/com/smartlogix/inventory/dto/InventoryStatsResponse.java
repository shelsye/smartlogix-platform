package com.smartlogix.inventory.dto;

public record InventoryStatsResponse(
        long totalProducts,
        long activeProducts,
        long outOfStockProducts,
        long lowStockProducts,
        long availableUnits,
        long reservedUnits
) {}
