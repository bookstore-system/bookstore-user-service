package com.notfound.userservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationEvent {

    private UUID eventId;
    private String type;
    private LocalDateTime occurredAt;
    private UUID userId;
    private String email;
    private String displayName;
    private String verificationUrl;
    private Integer expiresInMinutes;
}
