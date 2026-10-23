package com.easytravel.booking.config;

import com.easytravel.common.event.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * WHAT:   booking-service's RabbitMQ setup: the exchange it publishes to,
 *         the queue it listens on, and that queue's dead-letter queue.
 * WHY:    Declaring them as beans makes Spring create them on startup if
 *         they do not exist, so a fresh RabbitMQ needs no manual setup.
 *         Spring Boot automatically gives the JSON converter below to both
 *         the RabbitTemplate (publishing) and the listeners (consuming).
 *
 * @author AoceanP
 */
@Configuration
public class RabbitMQConfig
{
    /**
     * WHAT:   The exchange new bookings are announced on.
     * PRE:    none
     * RETURN: the durable topic exchange "booking.exchange"
     *
     * @return the exchange
     */
    @Bean
    public TopicExchange bookingExchange()
    {
        return new TopicExchange(MessagingConstants.BOOKING_EXCHANGE);
    }

    /**
     * WHAT:   The exchange payment results arrive on (declared by payment-service too).
     * WHY:    Declaring it on both sides means either service can start first.
     * PRE:    none
     * RETURN: the durable topic exchange "payment.exchange"
     *
     * @return the exchange
     */
    @Bean
    public TopicExchange paymentExchange()
    {
        return new TopicExchange(MessagingConstants.PAYMENT_EXCHANGE);
    }

    /**
     * WHAT:   The queue of payment results for booking-service.
     * WHY:    Durable, so results survive a RabbitMQ restart. Its
     *         dead-letter exchange catches messages the listener rejects
     *         (e.g. for a booking that does not exist), instead of retrying
     *         them forever.
     * PRE:    none
     * RETURN: the queue
     *
     * @return the queue
     */
    @Bean
    public Queue paymentQueue()
    {
        return QueueBuilder.durable(MessagingConstants.PAYMENT_QUEUE)
                .deadLetterExchange(MessagingConstants.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingConstants.PAYMENT_DLQ)
                .build();
    }

    /**
     * WHAT:   Connects the queue to the exchange for "payment.completed" messages.
     * WHY:    Without a binding, published messages have nowhere to go.
     * PRE:    both beans exist
     * RETURN: the binding
     *
     * @param paymentQueue    the queue
     * @param paymentExchange the exchange
     * @return the binding
     */
    @Bean
    public Binding paymentBinding(Queue paymentQueue, TopicExchange paymentExchange)
    {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(MessagingConstants.PAYMENT_ROUTING_KEY);
    }

    /**
     * WHAT:   The dead-letter exchange shared by both services.
     * PRE:    none
     * RETURN: the durable direct exchange "easytravel.dlx"
     *
     * @return the exchange
     */
    @Bean
    public DirectExchange deadLetterExchange()
    {
        return new DirectExchange(MessagingConstants.DEAD_LETTER_EXCHANGE);
    }

    /**
     * WHAT:   Where rejected payment results wait for a person to inspect them.
     * PRE:    none
     * RETURN: the durable dead-letter queue
     *
     * @return the queue
     */
    @Bean
    public Queue paymentDeadLetterQueue()
    {
        return QueueBuilder.durable(MessagingConstants.PAYMENT_DLQ).build();
    }

    /**
     * WHAT:   Routes rejected payment results into their dead-letter queue.
     * PRE:    both beans exist
     * RETURN: the binding
     *
     * @param paymentDeadLetterQueue the dead-letter queue
     * @param deadLetterExchange     the dead-letter exchange
     * @return the binding
     */
    @Bean
    public Binding paymentDeadLetterBinding(Queue paymentDeadLetterQueue, DirectExchange deadLetterExchange)
    {
        return BindingBuilder.bind(paymentDeadLetterQueue).to(deadLetterExchange).with(MessagingConstants.PAYMENT_DLQ);
    }

    /**
     * WHAT:   Sends and reads message bodies as JSON.
     * WHY:    Readable in the RabbitMQ web console, and language-neutral;
     *         Java serialization would be binary and brittle.
     * PRE:    none
     * RETURN: the Jackson 3 JSON converter
     *
     * @return the converter
     */
    @Bean
    public MessageConverter jsonMessageConverter()
    {
        return new JacksonJsonMessageConverter();
    }
}
