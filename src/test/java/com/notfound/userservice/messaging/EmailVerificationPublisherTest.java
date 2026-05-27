package com.notfound.userservice.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailVerificationPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private UserMessagingProperties properties;
    private EmailVerificationPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        properties = new UserMessagingProperties();
        properties.setEmailVerificationBaseUrl("https://nhasachcongdong.id.vn");
        publisher = new EmailVerificationPublisher(rabbitTemplate, properties);
    }

    @Test
    void publish_sendsEmailVerificationEventToEmailVerificationRoutingKey() {
        publisher.publish("u@example.com", "jwt.token");

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EmailVerificationEvent> eventCaptor = ArgumentCaptor.forClass(EmailVerificationEvent.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture(),
                any(MessagePostProcessor.class));

        assertEquals("bookstore.events", exchangeCaptor.getValue());
        assertEquals("user.email_verification", routingKeyCaptor.getValue());

        EmailVerificationEvent event = eventCaptor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals("user.email_verification", event.getType());
        assertEquals("u@example.com", event.getEmail());
        assertEquals(
                "https://nhasachcongdong.id.vn/api/v1/auth/confirm-email?token=jwt.token",
                event.getVerificationUrl());
        assertEquals(1440, event.getExpiresInMinutes());
    }
}
