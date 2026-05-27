package com.notfound.userservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.oauth.google")
public class GoogleOAuthProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String frontendRedirectUrl = "http://localhost:3000";

    public boolean hasClientId() {
        return clientId != null && !clientId.isBlank();
    }

    public boolean hasAuthorizationCodeConfig() {
        return hasClientId()
                && clientSecret != null && !clientSecret.isBlank()
                && redirectUri != null && !redirectUri.isBlank();
    }
}
