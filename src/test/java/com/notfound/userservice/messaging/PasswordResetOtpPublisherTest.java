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

class PasswordResetOtpPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private UserMessagingProperties properties;
    private PasswordResetOtpPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        properties = new UserMessagingProperties();
        publisher = new PasswordResetOtpPublisher(rabbitTemplate, properties);
    }

    @Test
    void publish_sendsOtpEventToPasswordResetRoutingKey() {
        publisher.publish("u@example.com", "123456");

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PasswordResetOtpEvent> eventCaptor = ArgumentCaptor.forClass(PasswordResetOtpEvent.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture(),
                any(MessagePostProcessor.class));

        assertEquals("bookstore.events", exchangeCaptor.getValue());
        assertEquals("user.password_reset", routingKeyCaptor.getValue());

        PasswordResetOtpEvent event = eventCaptor.getValue();
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
        assertEquals("user.password_reset", event.getType());
        assertEquals("u@example.com", event.getEmail());
        assertEquals("123456", event.getOtp());
        assertEquals(5, event.getExpiresInMinutes());
    }
}
