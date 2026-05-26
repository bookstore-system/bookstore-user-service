package com.notfound.userservice.model.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    BigDecimal totalRevenue;
    BigDecimal revenueGrowth;
    Long totalOrders;
    BigDecimal ordersGrowth;
    Long totalBooksInStock;
    Long lowStockCount;
    Long activeCustomers;
    Long newCustomers;
}
