package com.notfound.userservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String username;
    String email;
    String fullName;
    String phoneNumber;
    String role;
    String gender;
    String status;
    LocalDate dateOfBirth;
    Integer points;
    String membershipTier;
    Boolean isEmailVerified;
    String authProvider;
    LocalDateTime lastLogin;
    String avatar;
}
