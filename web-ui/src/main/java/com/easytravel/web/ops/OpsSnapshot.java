package com.easytravel.web.ops;

import com.easytravel.web.ops.RabbitAdminClient.QueueInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * WHAT:   The system's state at one moment: services, queue depths, and
 *         whether payments are paused.
 * WHY:    Built by OpsMonitor in the background every 2 seconds, so the page
 *         renders from memory and never waits on Eureka or RabbitMQ.
 *         null for a part means "could not be read".
 *
 * @param services       one entry per expected service
 * @param queues         the saga queues and their dead-letter queues, or null
 * @param paymentsPaused whether payments are paused, or null if unknown
 * @author AoceanP
 */
public record OpsSnapshot(List<Service> services, List<QueueInfo> queues, Boolean paymentsPaused)
{
    /** WHAT: the services the system is made of, in display order, with their ports. */
    static final String[][] EXPECTED = {
            { "api-gateway", "8080" }, { "discovery-server", "8761" }, { "inventory-service", "8081" },
            { "booking-service", "8082" }, { "payment-service", "8083" }, { "rabbitmq", "5672" }, { "web-ui", "9080" } };

    /** WHAT: the queues worth watching, in display order. */
    static final List<String> QUEUES = List.of(
            "payment.booking-created.queue", "booking.payment-completed.queue",
            "payment.refund-requested.queue", "booking.refund-completed.queue");

    /**
     * WHAT:   One service's health.
     * WHY:    Pages read only its components: Jakarta EL's record support
     *         resolves components, not extra methods on a record.
     *
     * @param name  e.g. "booking-service"
     * @param port  its port
     * @param state "up", "down", "paused" or "unknown" (also its CSS suffix)
     */
    public record Service(String name, String port, String state)
    {
    }

    /**
     * WHAT:   Builds the snapshot from what could be read.
     * WHY:    Eureka does not list itself or RabbitMQ, so those two are judged
     *         by whether their own APIs answered; the website is up by definition.
     * PRE:    any argument may be null (not readable)
     * RETURN: the snapshot
     *
     * @param registered     names Eureka reports UP (upper case), or null
     * @param queues         all queues from RabbitMQ, or null
     * @param paymentsPaused the pause switch, or null
     * @return the snapshot
     */
    static OpsSnapshot of(List<String> registered, List<QueueInfo> queues, Boolean paymentsPaused)
    {
        List<Service> services = new ArrayList<>();
        for (String[] s : EXPECTED)
        {
            String name = s[0];
            String state = switch (name)
            {
                case "web-ui" -> "up";
                case "discovery-server" -> registered == null ? "down" : "up";
                case "rabbitmq" -> queues == null ? "unknown" : "up";
                default -> registered == null ? "unknown"
                        : !registered.contains(name.toUpperCase(Locale.ROOT)) ? "down"
                        : "payment-service".equals(name) && Boolean.TRUE.equals(paymentsPaused) ? "paused"
                        : "up";
            };
            services.add(new Service(name, s[1], state));
        }
        List<QueueInfo> shown = null;
        if (queues != null)
        {
            shown = new ArrayList<>();
            for (String q : QUEUES)
            {
                shown.add(find(queues, q));
                shown.add(find(queues, q + ".dlq"));
            }
        }
        return new OpsSnapshot(List.copyOf(services), shown == null ? null : List.copyOf(shown), paymentsPaused);
    }

    /**
     * WHAT:   A queue's entry, or an empty one if RabbitMQ has not created it yet.
     * PRE:    queues is not null
     * RETURN: the entry
     *
     * @param queues all queues
     * @param name   the queue wanted
     * @return its entry
     */
    private static QueueInfo find(List<QueueInfo> queues, String name)
    {
        return queues.stream().filter(q -> name.equals(q.name())).findFirst().orElse(new QueueInfo(name, 0));
    }
}
