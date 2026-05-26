package com.notfound.userservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticResponse {
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private List<DailyRevenuePoint> breakdown;
    private List<PercentageDTO> revenueByPaymentMethod;
    private List<CategoryPerformanceDTO> categoryPerformance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenuePoint {
        private String date;
        private BigDecimal revenue;
        private Long orderCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PercentageDTO {
        private String category;
        private BigDecimal value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryPerformanceDTO {
        private String category;
        private BigDecimal revenue;
        private BigDecimal growth;
    }
}
