package com.easytravel.payment.config;

import com.easytravel.common.event.MessagingConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ setup for the PRODUCER side.
 *
 * <p>Declares the exchange we publish to, and — crucially — a JSON message converter so
 * our PaymentEvent record is serialized as readable JSON rather than Java's binary format.
 * The RabbitTemplate is the object we actually call to send messages.
 */
@Configuration
public class RabbitMQConfig {

    /**
     * A TopicExchange named "payment.exchange". A topic exchange routes messages to
     * queues based on a routing key pattern — flexible and the most common choice.
     * The name comes from the shared constant, so producer and consumer agree.
     */
    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(MessagingConstants.PAYMENT_EXCHANGE);
    }

    /**
     * Tells Spring AMQP to convert message bodies to/from JSON using Jackson.
     * Without this, Spring falls back to Java serialization (binary, brittle).
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * The RabbitTemplate is the send/receive helper. We wire our JSON converter into it
     * so every message it publishes is JSON-encoded automatically.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}