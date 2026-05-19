package com.notfound.userservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserBasicInfoResponse {
    UUID userId;
    String displayName;
    String avatarUrl;
}
