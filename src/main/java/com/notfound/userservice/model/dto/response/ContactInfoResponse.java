package com.notfound.userservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContactInfoResponse {
    UUID userId;
    String email;
    String phoneNumber;
    /**
     * Device token for push notifications (e.g., FCM token).
     * Currently null — will be populated when mobile push notification support is implemented.
     */
    String deviceToken;
}
