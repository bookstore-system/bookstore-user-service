package com.notfound.userservice.model.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRecentOrdersResponse {
    List<RecentOrder> orders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrder {
        String id;
        String orderCode;
        String customerName;
        BigDecimal total;
        String orderDate;
        String status;
    }
}
