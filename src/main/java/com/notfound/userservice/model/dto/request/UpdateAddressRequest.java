package com.notfound.userservice.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Request DTO để cập nhật thông tin địa chỉ giao hàng
 * Tất cả các trường đều optional, chỉ cập nhật những trường được gửi lên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAddressRequest {

    String recipientName;

    @Pattern(regexp = "^(\\+84|0)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    String phoneNumber;

    @Size(min = 1, message = "Địa chỉ chi tiết không được để trống")
    String street;

    @Size(min = 1, message = "Phường/Xã không được để trống")
    String ward;

    @Size(min = 1, message = "Quận/Huyện không được để trống")
    String district;

    @Size(min = 1, message = "Tỉnh/Thành phố không được để trống")
    String province;

    @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90 đến 90")
    BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180 đến 180")
    BigDecimal longitude;

    @NotNull(message = "Mã Tỉnh theo GHN không được để trống")
    Integer provinceId;

    @NotNull(message = "ID Quận/Huyện theo GHN không được để trống")
    Integer districtId;

    @NotBlank(message = "Mã Phường/Xã theo GHN không được để trống")
    String wardCode;
}
