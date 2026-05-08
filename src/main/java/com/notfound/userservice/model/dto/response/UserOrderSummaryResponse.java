package com.notfound.userservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserOrderSummaryResponse {
    UUID userId;
    Integer totalOrders;
    BigDecimal totalSpent;
    LocalDateTime lastOrderDate;
}
