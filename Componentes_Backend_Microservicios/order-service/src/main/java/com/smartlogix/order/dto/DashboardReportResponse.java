package com.smartlogix.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardReportResponse(
        long totalOrders,
        long salesCount,
        long pendingOrders,
        long deliveredOrders,
        long cancelledOrders,
        BigDecimal totalIncome,
        List<MetricValue> salesByRegion,
        List<MetricValue> salesByDay,
        List<MetricValue> salesByMonth,
        List<ProductSalesMetric> topProducts,
        Map<String, Long> ordersByStatus
) {}
