package com.notfound.userservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PasswordResetOtpPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final UserMessagingProperties properties;

    public void publish(String email, String otp) {
        PasswordResetOtpEvent event = PasswordResetOtpEvent.builder()
                .eventId(UUID.randomUUID())
                .type(properties.getRkPasswordReset())
                .occurredAt(LocalDateTime.now())
                .email(email)
                .otp(otp)
                .expiresInMinutes(properties.getOtpExpiryMinutes())
                .build();

        rabbitTemplate.convertAndSend(
                properties.getExchangeEvents(),
                properties.getRkPasswordReset(),
                event,
                this::removeJavaTypeHeaders);

        log.info("Published password reset OTP event. eventId={} email={} exchange={} routingKey={}",
                event.getEventId(),
                email,
                properties.getExchangeEvents(),
                properties.getRkPasswordReset());
    }

    private Message removeJavaTypeHeaders(Message message) {
        message.getMessageProperties().getHeaders().remove("__TypeId__");
        message.getMessageProperties().getHeaders().remove("__ContentTypeId__");
        message.getMessageProperties().getHeaders().remove("__KeyTypeId__");
        return message;
    }
}
