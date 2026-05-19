package com.notfound.userservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDetailResponse {

    UUID id;
    String username;
    String email;
    String fullName;
    String phoneNumber;
    String gender;
    String avatarUrl;
    String role;
    String status;

    // Thông tin bổ sung
    LocalDate dateOfBirth;
    Integer points;
    String membershipTier;
    Boolean isEmailVerified;
    String authProvider;
    String providerId;

    // Thống kê
    Integer totalOrders;
    BigDecimal totalSpent;
    Integer totalReviews;
    LocalDateTime lastOrderDate;

    // Địa chỉ
    List<AddressResponse> addresses;

    // Thời gian
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime lastLoginAt;
}
