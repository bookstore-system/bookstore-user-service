package com.notfound.userservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@EnableConfigurationProperties(UserMessagingProperties.class)
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UserMessagingConfig {

    public static final String EMAIL_VERIFICATION_QUEUE = "notification.email_verification_events";
    public static final String EMAIL_VERIFICATION_ROUTING_KEY = "user.email_verification";

    @Bean
    public ObjectMapper userMessagingObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public MessageConverter userMessageConverter(
            @Qualifier("userMessagingObjectMapper") ObjectMapper userMessagingObjectMapper) {
        return new Jackson2JsonMessageConverter(userMessagingObjectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter userMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(userMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public TopicExchange userEventsExchange(UserMessagingProperties properties) {
        return new TopicExchange(properties.getExchangeEvents(), true, false);
    }

    @Bean
    public Queue passwordResetQueue(UserMessagingProperties properties) {
        return QueueBuilder.durable(properties.getQueuePasswordReset()).build();
    }

    @Bean
    public Queue emailVerificationQueue() {
        return QueueBuilder.durable(EMAIL_VERIFICATION_QUEUE).build();
    }

    @Bean
    public Binding passwordResetBinding(
            Queue passwordResetQueue,
            TopicExchange userEventsExchange,
            UserMessagingProperties properties) {
        return BindingBuilder.bind(passwordResetQueue)
                .to(userEventsExchange)
                .with(properties.getRkPasswordReset());
    }

    @Bean
    public Binding emailVerificationBinding(
            Queue emailVerificationQueue,
            TopicExchange userEventsExchange) {
        return BindingBuilder.bind(emailVerificationQueue)
                .to(userEventsExchange)
                .with(EMAIL_VERIFICATION_ROUTING_KEY);
    }
}
