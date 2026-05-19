package com.notfound.userservice.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {
    String fullName;

    @Email(message = "Invalid email format")
    String email;

    @Pattern(regexp = "^(\\+84|0)[0-9]{9}$", message = "Invalid phone number")
    String phoneNumber;

    @Pattern(regexp = "^(Male|Female|Other)$")
    String gender;

    LocalDate dateOfBirth;
}
