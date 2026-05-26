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
public class DashboardTopCategoriesResponse {
    List<TopCategory> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCategory {
        String categoryId;
        String categoryName;
        BigDecimal percentage;
        BigDecimal totalSales;
    }
}
