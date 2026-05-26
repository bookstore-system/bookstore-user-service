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
public class DashboardSalesTrendResponse {
    List<SalesTrendData> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesTrendData {
        String month;
        String monthName;
        BigDecimal sales;
        Long orders;
        Long customers;
    }
}
