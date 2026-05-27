package com.notfound.userservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailVerificationPublisher {

    private static final String CONFIRM_EMAIL_PATH = "/api/v1/auth/confirm-email";
    private static final int EXPIRES_IN_MINUTES = 1440;

    private final RabbitTemplate rabbitTemplate;
    private final UserMessagingProperties properties;

    public void publish(String email, String token) {
        String verificationUrl = buildVerificationUrl(token);
        EmailVerificationEvent event = EmailVerificationEvent.builder()
                .eventId(UUID.randomUUID())
                .type(UserMessagingConfig.EMAIL_VERIFICATION_ROUTING_KEY)
                .occurredAt(LocalDateTime.now())
                .email(email)
                .verificationUrl(verificationUrl)
                .expiresInMinutes(EXPIRES_IN_MINUTES)
                .build();

        rabbitTemplate.convertAndSend(
                properties.getExchangeEvents(),
                UserMessagingConfig.EMAIL_VERIFICATION_ROUTING_KEY,
                event,
                this::removeJavaTypeHeaders);

        log.info("Published email verification event. eventId={} email={} exchange={} routingKey={}",
                event.getEventId(),
                email,
                properties.getExchangeEvents(),
                UserMessagingConfig.EMAIL_VERIFICATION_ROUTING_KEY);
    }

    private String buildVerificationUrl(String token) {
        String baseUrl = properties.getEmailVerificationBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        return normalizedBaseUrl
                + CONFIRM_EMAIL_PATH
                + "?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private Message removeJavaTypeHeaders(Message message) {
        message.getMessageProperties().getHeaders().remove("__TypeId__");
        message.getMessageProperties().getHeaders().remove("__ContentTypeId__");
        message.getMessageProperties().getHeaders().remove("__KeyTypeId__");
        return message;
    }
}
