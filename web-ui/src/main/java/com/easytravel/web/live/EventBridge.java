package com.easytravel.web.live;

import com.easytravel.web.book.Money;
import com.easytravel.web.ops.OpsLog;
import com.easytravel.web.ops.OpsMonitor;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.faces.push.Push;
import jakarta.faces.push.PushContext;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WHAT:   Listens to both sagas (booking/payment and cancel/refund) on
 *         RabbitMQ: tells the right browser, through a Faces websocket, that
 *         its booking changed, and feeds the Ops room. Also sends the Ops
 *         room's two test messages (a duplicate and a bad message).
 * WHY:    The website is one more consumer of the same events: it gets its own
 *         temporary queue bound to both exchanges, so booking-service and
 *         payment-service did not change at all to support it (publish and
 *         subscribe). Without it the pages would have to poll.
 *         If RabbitMQ is not up yet, it retries every 5 seconds; once connected,
 *         the client reconnects by itself after a broker restart.
 *
 * @author AoceanP
 */
@ApplicationScoped
public class EventBridge
{
    /** WHAT: the logger. */
    private static final Logger LOG = Logger.getLogger(EventBridge.class.getName());

    /** WHAT: pause between connection attempts while RabbitMQ is not reachable. */
    private static final long RETRY_SECONDS = 5;

    /** WHAT: the class name Spring's JSON converter in booking-service reads from the __TypeId__ header. */
    private static final String PAYMENT_EVENT_TYPE = "com.easytravel.common.event.PaymentEvent";

    @Inject
    @ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost")
    String host;

    @Inject
    @ConfigProperty(name = "rabbitmq.port", defaultValue = "5672")
    int port;

    /** WHAT: RabbitMQ's local-only default account; override with RABBITMQ_USERNAME / RABBITMQ_PASSWORD. */
    @Inject
    @ConfigProperty(name = "rabbitmq.username", defaultValue = "guest")
    String username;

    @Inject
    @ConfigProperty(name = "rabbitmq.password", defaultValue = "guest")
    String password;

    /** WHAT: whether the Ops room may send test messages (docker-compose turns it on). */
    @Inject
    @ConfigProperty(name = "easytravel.demo-controls", defaultValue = "false")
    boolean demoControls;

    /** WHAT: what has been seen per booking (for the customer pages). */
    @Inject
    BookingTracker tracker;

    /** WHAT: the Ops room's feed, counters and timings. */
    @Inject
    OpsLog opsLog;

    /** WHAT: the customer pages' push channel (one browser per booking). */
    @Inject
    @Push(channel = Messaging.PUSH_CHANNEL)
    PushContext push;

    /** WHAT: the Ops room's push channel (every open Ops room). */
    @Inject
    @Push(channel = Messaging.OPS_CHANNEL)
    PushContext opsPush;

    /** WHAT: the server's managed scheduler, for connection retries. */
    @Resource
    ManagedScheduledExecutorService scheduler;

    /** WHAT: the server's managed thread factory, so RabbitMQ's threads run inside the container. */
    @Resource
    ManagedThreadFactory threads;

    /** WHAT: reads the event JSON into records. */
    private final Jsonb json = JsonbBuilder.create();

    /** WHAT: ids of bookings the Ops room invented for bad messages; they never exist. */
    private final Set<String> badIds = ConcurrentHashMap.newKeySet();

    /** WHAT: the open connection, or null while not connected. */
    private volatile Connection connection;

    /** WHAT: a separate channel for sending; RabbitMQ channels must not be shared between threads. */
    private Channel publisher;

    /** WHAT: the last payment result exactly as it arrived (body and headers), for "send a duplicate". */
    private volatile Delivery lastPayment;

    /**
     * WHAT:   Starts listening once the web application has started.
     * WHY:    Connecting in the background keeps a slow or missing RabbitMQ from
     *         blocking or failing the website's start-up.
     * PRE:    called by CDI when the application scope begins
     * POST:   a connection attempt is scheduled
     *
     * @param ignored the start-up event
     */
    void start(@Observes @Initialized(ApplicationScoped.class) Object ignored)
    {
        scheduler.execute(this::connect);
    }

    /**
     * WHAT:   Whether the website is currently receiving live events.
     * PRE:    none
     * RETURN: true while connected to RabbitMQ
     *
     * @return whether live updates work
     */
    public boolean isConnected()
    {
        Connection c = connection;
        return c != null && c.isOpen();
    }

    /**
     * WHAT:   Whether the Ops room's test messages are allowed.
     * RETURN: true when easytravel.demo-controls is on
     *
     * @return whether allowed
     */
    public boolean isDemoControls()
    {
        return demoControls;
    }

    /**
     * WHAT:   Connects, declares a private queue, binds it and starts consuming.
     * PRE:    none
     * POST:   consuming events, or another attempt is scheduled
     */
    private void connect()
    {
        try
        {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setThreadFactory(threads);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setConnectionTimeout(3000);

            Connection c = factory.newConnection("easytravel-web-ui");
            Channel channel = c.createChannel();

            // Same type and durability as the services declare, so this is a no-op if they exist.
            channel.exchangeDeclare(Messaging.BOOKING_EXCHANGE, BuiltinExchangeType.TOPIC, true);
            channel.exchangeDeclare(Messaging.PAYMENT_EXCHANGE, BuiltinExchangeType.TOPIC, true);

            // A server-named, exclusive, auto-deleted queue: it lives only while the website runs.
            String queue = channel.queueDeclare().getQueue();
            channel.queueBind(queue, Messaging.BOOKING_EXCHANGE, Messaging.BOOKING_CREATED_KEY);
            channel.queueBind(queue, Messaging.PAYMENT_EXCHANGE, Messaging.PAYMENT_COMPLETED_KEY);
            channel.queueBind(queue, Messaging.BOOKING_EXCHANGE, Messaging.REFUND_REQUESTED_KEY);
            channel.queueBind(queue, Messaging.PAYMENT_EXCHANGE, Messaging.REFUNDED_KEY);
            channel.basicConsume(queue, true, (tag, delivery) -> onEvent(delivery), tag -> { });

            synchronized (this)
            {
                publisher = c.createChannel();
            }
            connection = c;
            LOG.info(() -> "Listening for booking events on RabbitMQ at " + host + ":" + port);
        }
        catch (IOException | TimeoutException e)
        {
            LOG.info(() -> "RabbitMQ not reachable yet (" + e.getMessage() + "); retrying in " + RETRY_SECONDS + " s");
            scheduler.schedule(this::connect, RETRY_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * WHAT:   Handles one event: records it for the pages, then notifies them.
     * WHY:    A malformed message is logged and skipped; it must never stop the consumer.
     * PRE:    called by the RabbitMQ client for each delivery
     * POST:   tracker and Ops log updated; the watching browser and Ops rooms notified
     *
     * @param delivery the message
     */
    void onEvent(Delivery delivery)
    {
        String key = delivery.getEnvelope().getRoutingKey();
        String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
        try
        {
            Optional<String> watcher = Optional.empty();
            switch (key)
            {
                case Messaging.BOOKING_CREATED_KEY ->
                {
                    BookingCreatedMessage m = json.fromJson(body, BookingCreatedMessage.class);
                    watcher = tracker.created(m.bookingId());
                    opsLog.bookingCreated(m.bookingId(), Money.format(m.amount()));
                }
                case Messaging.PAYMENT_COMPLETED_KEY ->
                {
                    PaymentMessage m = json.fromJson(body, PaymentMessage.class);
                    if (badIds.remove(m.bookingId()))
                    {
                        opsLog.control("bad PaymentEvent", "booking " + m.bookingId().substring(0, 8)
                                + " does not exist: booking-service retries 3 times, then dead-letters it");
                    }
                    else
                    {
                        lastPayment = delivery;
                        watcher = tracker.paid(m.bookingId(), m.success(), m.reason());
                        opsLog.paymentResult(m.bookingId(), m.success(), m.reason());
                    }
                }
                case Messaging.REFUND_REQUESTED_KEY ->
                {
                    RefundMessage m = json.fromJson(body, RefundMessage.class);
                    watcher = tracker.refundRequested(m.bookingId());
                    opsLog.refundRequested(m.bookingId());
                }
                case Messaging.REFUNDED_KEY ->
                {
                    RefundMessage m = json.fromJson(body, RefundMessage.class);
                    watcher = tracker.refunded(m.bookingId(), m.success(), m.reason());
                    opsLog.refundResult(m.bookingId(), m.success(), m.reason());
                }
                default -> LOG.fine(() -> "Ignoring routing key " + key);
            }
            watcher.ifPresent(this::notifyWatcher);
            OpsMonitor.notifyOpsRooms(opsPush);
        }
        catch (JsonbException | NullPointerException bad)
        {
            LOG.log(Level.WARNING, "Skipping an event that could not be read (" + key + ")", bad);
        }
    }

    /**
     * WHAT:   Sends the last payment result again, unchanged (the "duplicate" test).
     * WHY:    Shows idempotency: booking-service must ignore it, and no
     *         customer is charged or confirmed twice.
     * PRE:    demo controls are on
     * RETURN: the short id of the booking it repeats
     * THROWS: IllegalStateException if not allowed, not connected, or no payment seen yet
     *
     * @return the booking's short id
     * @throws IllegalStateException if it cannot be sent
     */
    public synchronized String sendDuplicatePayment()
    {
        requireDemoControls();
        Delivery last = lastPayment;
        if (last == null)
        {
            throw new IllegalStateException("No payment has happened yet. Make a booking first.");
        }
        publish(Messaging.PAYMENT_EXCHANGE, Messaging.PAYMENT_COMPLETED_KEY, last.getProperties(), last.getBody());
        PaymentMessage m = json.fromJson(new String(last.getBody(), StandardCharsets.UTF_8), PaymentMessage.class);
        return m.bookingId().substring(0, 8);
    }

    /**
     * WHAT:   Sends a payment result for a booking that does not exist (the "bad message" test).
     * WHY:    Shows the failure path: booking-service retries 3 times, then the
     *         message waits in its dead-letter queue instead of looping forever.
     * PRE:    demo controls are on
     * RETURN: the short id of the invented booking
     * THROWS: IllegalStateException if not allowed or not connected
     *
     * @return the invented booking's short id
     * @throws IllegalStateException if it cannot be sent
     */
    public synchronized String sendBadPayment()
    {
        requireDemoControls();
        String id = UUID.randomUUID().toString();
        badIds.add(id);
        String body = json.toJson(new PaymentMessage(id, "tx-ops-test", true, java.math.BigDecimal.ONE, null));
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .headers(Map.of("__TypeId__", PAYMENT_EVENT_TYPE))
                .build();
        publish(Messaging.PAYMENT_EXCHANGE, Messaging.PAYMENT_COMPLETED_KEY, props, body.getBytes(StandardCharsets.UTF_8));
        return id.substring(0, 8);
    }

    /**
     * WHAT:   Publishes one message on the sending channel.
     * PRE:    caller holds the lock
     * POST:   the message is with RabbitMQ
     * THROWS: IllegalStateException if not connected or the send fails
     *
     * @param exchange the exchange
     * @param key      the routing key
     * @param props    the message properties
     * @param body     the body
     */
    private void publish(String exchange, String key, AMQP.BasicProperties props, byte[] body)
    {
        if (publisher == null || !isConnected())
        {
            throw new IllegalStateException("RabbitMQ is not connected right now.");
        }
        try
        {
            publisher.basicPublish(exchange, key, props, body);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Could not send the message: " + e.getMessage(), e);
        }
    }

    /**
     * WHAT:   Refuses test messages unless demo controls are on.
     * PRE:    none
     * THROWS: IllegalStateException if off
     *
     * @throws IllegalStateException if demo controls are off
     */
    private void requireDemoControls()
    {
        if (!demoControls)
        {
            throw new IllegalStateException("Demo controls are turned off on this server.");
        }
    }

    /**
     * WHAT:   Pushes "updated" to one browser.
     * WHY:    The browser answers with a small Faces ajax request that re-reads
     *         the booking, so the page always shows booking-service's own state.
     * PRE:    watcher is a push key a page registered
     * POST:   the message is sent, or the failure is logged
     *
     * @param watcher the browser's push key
     */
    private void notifyWatcher(String watcher)
    {
        try
        {
            push.send(Messaging.PUSH_UPDATED, watcher);
        }
        catch (RuntimeException e)
        {
            LOG.log(Level.WARNING, "Could not push an update to the browser", e);
        }
    }

    /**
     * WHAT:   Closes the connection when the website stops.
     * PRE:    called by CDI on shutdown
     * POST:   no connection remains open
     */
    @PreDestroy
    void stop()
    {
        Connection c = connection;
        connection = null;
        if (c != null)
        {
            try
            {
                c.close(2000);
            }
            catch (IOException | RuntimeException e)
            {
                LOG.fine("RabbitMQ connection already closed");
            }
        }
    }
}
