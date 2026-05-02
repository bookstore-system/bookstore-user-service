package com.notfound.userservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Response DTO chứa chi tiết địa chỉ giao hàng dùng cho internal service calls (e.g. Order Service)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressDetailResponse {
    UUID id;
    String recipientName;
    String phoneNumber;
    String fullAddress;
    String province;
    String district;
    String ward;
}
