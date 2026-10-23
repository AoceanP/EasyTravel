package com.easytravel.payment.controller;

import com.easytravel.payment.messaging.BookingCreatedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WHAT:   Demo-only switch that pauses and resumes taking payments.
 * WHY:    Lets the Ops room show resilience: while paused, new bookings wait
 *         in the durable RabbitMQ queue, and on resume they are all charged,
 *         none lost. Pausing stops only the queue listener; the service stays up.
 *
 *         It exists only when easytravel.demo-controls=true, and its paths
 *         start with /admin, which the API gateway does not route: only
 *         services inside the Docker network (the website) can reach it.
 *
 *         GET  /admin/payments          {"paused": false}
 *         POST /admin/payments/pause    stop charging
 *         POST /admin/payments/resume   charge everything that waited
 *
 * @author AoceanP
 */
@RestController
@ConditionalOnProperty(name = "easytravel.demo-controls", havingValue = "true")
public class DemoControlController
{
    /** WHAT: logs every pause and resume. */
    private static final Logger log = LoggerFactory.getLogger(DemoControlController.class);

    /**
     * WHAT:   Whether payments are paused.
     *
     * @param paused true while the listener is stopped
     */
    public record PauseState(boolean paused)
    {
    }

    /** WHAT: Spring's registry of RabbitMQ listeners. */
    private final RabbitListenerEndpointRegistry registry;

    /**
     * WHAT:   Creates the controller.
     * PRE:    registry is not null
     *
     * @param registry the listener registry
     */
    public DemoControlController(RabbitListenerEndpointRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * WHAT:   Reports whether payments are paused.
     * PRE:    none
     * RETURN: the state
     *
     * @return the state
     */
    @GetMapping("/admin/payments")
    public PauseState state()
    {
        return new PauseState(!listener().isRunning());
    }

    /**
     * WHAT:   Stops taking payments; new bookings wait in the queue.
     * PRE:    none
     * POST:   the booking-created listener is stopped
     * RETURN: the new state
     *
     * @return the state
     */
    @PostMapping("/admin/payments/pause")
    public PauseState pause()
    {
        listener().stop();
        log.warn("payments PAUSED by the demo controls; bookings will wait in the queue");
        return state();
    }

    /**
     * WHAT:   Starts taking payments again; waiting bookings are charged.
     * PRE:    none
     * POST:   the booking-created listener is running
     * RETURN: the new state
     *
     * @return the state
     */
    @PostMapping("/admin/payments/resume")
    public PauseState resume()
    {
        listener().start();
        log.warn("payments RESUMED by the demo controls");
        return state();
    }

    /**
     * WHAT:   The listener that charges new bookings.
     * PRE:    BookingCreatedListener is registered
     * RETURN: its container
     * THROWS: IllegalStateException if it is missing
     *
     * @return the container
     */
    private MessageListenerContainer listener()
    {
        MessageListenerContainer c = registry.getListenerContainer(BookingCreatedListener.LISTENER_ID);
        if (c == null)
        {
            throw new IllegalStateException("listener " + BookingCreatedListener.LISTENER_ID + " not found");
        }
        return c;
    }
}
