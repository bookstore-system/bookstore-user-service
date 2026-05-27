package com.notfound.userservice.messaging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.messaging")
public class UserMessagingProperties {

    private boolean enabled = true;
    private String exchangeEvents = "bookstore.events";
    private String queuePasswordReset = "notification.password_reset_events";
    private String rkPasswordReset = "user.password_reset";
    private int otpExpiryMinutes = 5;
    private String emailVerificationBaseUrl = "http://localhost:8080";
}
