package com.notfound.userservice.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password;

    String fullName;

    // Phone number is optional, only validate format if provided
    @Pattern(regexp = "^(\\+84|0)[0-9]{9}$", message = "Invalid phone number")
    String phoneNumber;

    LocalDate dateOfBirth;
}
