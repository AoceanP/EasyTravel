package com.easytravel.payment.config;

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
 * WHAT:   payment-service's RabbitMQ setup: the queue of new bookings it
 *         listens on (with a dead-letter queue), and the exchange it
 *         publishes payment results to.
 * WHY:    Declared as beans so a fresh RabbitMQ is set up automatically on
 *         startup. The exchanges are declared by both services, so either
 *         can start first.
 *
 * @author AoceanP
 */
@Configuration
public class RabbitMQConfig
{
    /**
     * WHAT:   The exchange payment results are published to.
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
     * WHAT:   The exchange new bookings arrive on.
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
     * WHAT:   payment-service's queue of bookings to charge.
     * WHY:    Durable, so a booking waiting for payment survives a RabbitMQ
     *         restart; rejected messages go to the dead-letter queue.
     * PRE:    none
     * RETURN: the queue
     *
     * @return the queue
     */
    @Bean
    public Queue bookingCreatedQueue()
    {
        return QueueBuilder.durable(MessagingConstants.BOOKING_CREATED_QUEUE)
                .deadLetterExchange(MessagingConstants.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(MessagingConstants.BOOKING_CREATED_DLQ)
                .build();
    }

    /**
     * WHAT:   Connects the queue to the exchange for "booking.created" messages.
     * PRE:    both beans exist
     * RETURN: the binding
     *
     * @param bookingCreatedQueue the queue
     * @param bookingExchange     the exchange
     * @return the binding
     */
    @Bean
    public Binding bookingCreatedBinding(Queue bookingCreatedQueue, TopicExchange bookingExchange)
    {
        return BindingBuilder.bind(bookingCreatedQueue).to(bookingExchange)
                .with(MessagingConstants.BOOKING_CREATED_ROUTING_KEY);
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
     * WHAT:   Where rejected bookings wait for a person to inspect them.
     * PRE:    none
     * RETURN: the durable dead-letter queue
     *
     * @return the queue
     */
    @Bean
    public Queue bookingCreatedDeadLetterQueue()
    {
        return QueueBuilder.durable(MessagingConstants.BOOKING_CREATED_DLQ).build();
    }

    /**
     * WHAT:   Routes rejected bookings into their dead-letter queue.
     * PRE:    both beans exist
     * RETURN: the binding
     *
     * @param bookingCreatedDeadLetterQueue the dead-letter queue
     * @param deadLetterExchange            the dead-letter exchange
     * @return the binding
     */
    @Bean
    public Binding bookingCreatedDeadLetterBinding(Queue bookingCreatedDeadLetterQueue, DirectExchange deadLetterExchange)
    {
        return BindingBuilder.bind(bookingCreatedDeadLetterQueue).to(deadLetterExchange)
                .with(MessagingConstants.BOOKING_CREATED_DLQ);
    }

    /**
     * WHAT:   Sends and reads message bodies as JSON.
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
