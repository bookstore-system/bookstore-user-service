package com.notfound.userservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserManagementResponse {
    UUID id;
    String username;
    String email;
    String fullName;
    String phoneNumber;
    String gender;
    String avatarUrl;
    String role;
    String status;
    LocalDate dateOfBirth;
    Integer points;
    String membershipTier;
    Boolean isEmailVerified;
    LocalDateTime createdAt;
    LocalDateTime lastLogin;
    Integer totalOrders;
    BigDecimal totalSpent;
}
