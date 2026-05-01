package com.notfound.userservice.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email;

    String fullName;

    @Pattern(regexp = "^(\\+84|0)[0-9]{9}$", message = "Invalid phone number format")
    String phoneNumber;

    @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female or Other")
    String gender;

    String avatarUrl;

    LocalDate dateOfBirth;

    @Pattern(regexp = "^(GUEST|CUSTOMER|ADMIN)$", message = "Role must be GUEST, CUSTOMER or ADMIN")
    String role; // Default sẽ là CUSTOMER nếu không có
}
