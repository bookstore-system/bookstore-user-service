package com.notfound.userservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address {

    @Id
    @UuidGenerator
    UUID id;

    @Column(nullable = false)
    String recipientName;

    @Column(nullable = false)
    String phoneNumber;

    @Column(nullable = false)
    String street;

    @Column(nullable = false)
    String ward;

    @Column(nullable = false)
    String district;

    @Column(nullable = false)
    String province;

    @Column(precision = 10, scale = 7)
    BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    BigDecimal longitude;

    // --- BỔ SUNG CHO GHN (Giao Hàng Nhanh) ---
    @Column(name = "ghn_province_id")
    Integer provinceId; // ID Tỉnh theo GHN (để lọc)

    @Column(name = "ghn_district_id")
    Integer districtId; // ID Quận/Huyện theo GHN (BẮT BUỘC để tính phí)

    @Column(name = "ghn_ward_code")
    String wardCode; // Mã Phường/Xã theo GHN (BẮT BUỘC)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}
