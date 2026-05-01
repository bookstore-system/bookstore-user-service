package com.notfound.userservice.model.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {
    String fullName;

    @Pattern(regexp = "^(\\+84|0)[0-9]{9}$", message = "Invalid phone number")
    String phoneNumber;

    @Pattern(regexp = "^(Male|Female|Other)$")
    String gender;

    LocalDate dateOfBirth;

    MultipartFile avatar;
}
