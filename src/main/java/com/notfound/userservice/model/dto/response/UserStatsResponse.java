package com.notfound.userservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStatsResponse {

    // Thống kê tổng quan
    Long totalUsers;
    Long activeUsers;
    Long inactiveUsers;
    Long bannedUsers;

    // Thống kê theo role
    Long totalAdmins;
    Long totalCustomers;
    Long totalGuests;

    // Thống kê người dùng mới
    Long newUsersThisMonth;
    Long newUsersThisWeek;
    Long newUsersToday;

    // Thống kê doanh thu
    BigDecimal totalRevenue;
    BigDecimal avgRevenuePerUser;
    BigDecimal avgOrderValue;
    Long totalOrders;

    // Top users
    List<TopUserResponse> topSpenders;
    List<TopUserResponse> topBuyers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TopUserResponse {
        String userId;
        String username;
        String email;
        String fullName;
        String avatarUrl;
        Integer totalOrders;
        BigDecimal totalSpent;
    }
}
