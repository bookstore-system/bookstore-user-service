package com.notfound.userservice.model.entity;

import com.notfound.userservice.model.enums.AuthProvider;
import com.notfound.userservice.model.enums.MembershipTier;
import com.notfound.userservice.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(exclude = {"addresses", "wishlist"})
public class User {

    @Id
    @UuidGenerator
    UUID id;

    @Column(unique = true, nullable = false)
    String username;

    @Column(nullable = false)
    String password;

    @Column(unique = true, nullable = false)
    String email;

    @Column
    String fullName;

    @Column
    String phoneNumber;

    @Column
    String gender;

    @Column
    String avatar_url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    @Column(length = 20)
    String status; // active, inactive, banned

    // 1. Audit
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at",
            columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
    LocalDateTime updatedAt;

    @Column(name = "last_login")
    LocalDateTime lastLogin;

    // 2. Loyalty (Khách hàng thân thiết)
    @Builder.Default
    @Column(name = "points", nullable = false)
    Integer points = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_tier")
    MembershipTier membershipTier = MembershipTier.BRONZE;

    // 3. Marketing & Profile
    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Builder.Default
    @Column(name = "is_email_verified")
    Boolean isEmailVerified = false;

    // 4. Social Login
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id")
    String providerId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Address> addresses;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Wishlist wishlist;

    public User(String username, String password, String email, Role role, String avatar_url) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.avatar_url = avatar_url;
    }
}
