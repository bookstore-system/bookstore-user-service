package com.notfound.userservice.model.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class DashboardBookResponse {
    String id;
    String title;
    Double price;
    Double discountPrice;
    Integer stockQuantity;
    Double averageRating;
    Integer reviewCount;
    Integer totalOrders;
    List<String> authorNames;
    List<CategoryInfo> categories;

    @Data
    public static class CategoryInfo {
        String id;
        String name;
    }
}
