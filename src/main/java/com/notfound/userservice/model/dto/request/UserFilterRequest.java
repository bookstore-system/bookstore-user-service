package com.notfound.userservice.model.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFilterRequest {

    String search; // Tìm kiếm theo username, email, fullName

    String role; // Filter theo role: GUEST, CUSTOMER, ADMIN

    String status; // Filter theo status: active, inactive, banned

    String sortBy; // Sắp xếp theo: username, email, createdAt, totalOrders, totalSpent

    String sortDirection; // asc hoặc desc

    Integer page; // Số trang (bắt đầu từ 0)

    Integer size; // Số lượng items mỗi trang
}
