package com.notfound.userservice.model.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTopBooksResponse {
    List<TopSellingBook> books;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopSellingBook {
        String id;
        String title;
        List<String> authorNames;
        List<String> categoryNames;
        Double price;
        Double discountPrice;
        Double averageRating;
        Integer reviewCount;
        Integer soldQuantity;
    }
}
