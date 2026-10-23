package com.easytravel.web.ops;

import com.easytravel.web.live.EventBridge;
import com.easytravel.web.ops.RabbitAdminClient.QueueInfo;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * WHAT:   The Ops room page: counters, services, queues, the live event feed,
 *         saga timings, and the "break it" controls.
 * WHY:    Reads only from memory (OpsMonitor's snapshot and OpsLog), so it
 *         renders instantly even while a dependency is down; the controls are
 *         the only calls it makes, and each reports its outcome in plain words.
 *
 * @author AoceanP
 */
@Named
@ViewScoped
public class OpsView implements Serializable
{
    /** WHAT: how many timing bars are drawn. */
    static final int BARS = 16;

    /** WHAT: how the start time is shown. */
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    @Inject
    OpsMonitor monitor;

    @Inject
    OpsLog log;

    @Inject
    EventBridge events;

    @Inject
    @RestClient
    PaymentsAdminClient payments;

    private String message;
    private boolean messageBad;

    /**
     * WHAT:   One counter card.
     *
     * @param label the caption
     * @param value the number
     * @param sub   the line under it
     * @param css   its colour class
     */
    public record Kpi(String label, String value, String sub, String css)
    {
    }

    /**
     * WHAT:   One queue row.
     *
     * @param name     the queue name
     * @param messages messages waiting
     * @param width    bar width, e.g. "40%"
     * @param css      row classes (dead-letter queues are styled apart)
     */
    public record QueueRow(String name, int messages, String width, String css)
    {
    }

    /**
     * WHAT:   One timing bar.
     *
     * @param ms     the saga's time in milliseconds
     * @param height bar height, e.g. "62%"
     */
    public record Bar(long ms, String height)
    {
    }

    // ------------------------------------------------------------- controls

    /**
     * WHAT:   Pauses payments, or resumes them if paused.
     * WHY:    While paused, new bookings wait in the durable queue; on resume
     *         they are all charged. Nothing is lost: that is what the queue is for.
     * PRE:    none
     * POST:   payment-service's listener is switched and the snapshot refreshed,
     *         or the message says why not
     */
    public void togglePause()
    {
        if (!events.isDemoControls())
        {
            say("Demo controls are turned off on this server.", true);
            return;
        }
        boolean wasPaused = isPaused();
        try
        {
            boolean nowPaused = (wasPaused ? payments.resume() : payments.pause()).paused();
            log.control(nowPaused ? "payments paused" : "payments resumed",
                    nowPaused ? "new bookings will wait in payment.booking-created.queue"
                              : "waiting bookings are being charged now");
            monitor.refresh();
            say(nowPaused ? "Payments paused. Book something on the Book page and watch it wait in the queue."
                          : "Payments resumed. Everything that waited is being charged.", false);
        }
        catch (RuntimeException e)
        {
            say("payment-service didn't answer, so nothing changed. Try again in a moment.", true);
        }
    }

    /**
     * WHAT:   Sends the last payment result again.
     * PRE:    none
     * POST:   the duplicate is on its way, or the message says why not
     */
    public void sendDuplicate()
    {
        try
        {
            String id = events.sendDuplicatePayment();
            log.control("duplicate sent", "the payment result for " + id + " was sent again");
            say("Sent booking " + id + "'s payment result a second time. booking-service ignores it: "
                    + "nobody is charged or confirmed twice.", false);
        }
        catch (IllegalStateException e)
        {
            say(e.getMessage(), true);
        }
    }

    /**
     * WHAT:   Sends a payment result for a booking that does not exist.
     * PRE:    none
     * POST:   the bad message is on its way, or the message says why not
     */
    public void sendBadMessage()
    {
        try
        {
            String id = events.sendBadPayment();
            log.control("bad message sent", "a payment result for booking " + id + ", which does not exist");
            say("Sent a payment result for booking " + id + ", which doesn't exist. booking-service tries it "
                    + "3 times, then parks it: watch the dead-letter count go up in a couple of seconds.", false);
        }
        catch (IllegalStateException e)
        {
            say(e.getMessage(), true);
        }
    }

    /**
     * WHAT:   Sets the status line under the controls.
     * PRE:    text is not null
     * POST:   message updated
     *
     * @param text the message
     * @param bad  whether it reports a problem
     */
    private void say(String text, boolean bad)
    {
        message = text;
        messageBad = bad;
    }

    // ------------------------------------------------------------- what the page reads

    /**
     * WHAT:   The counter cards.
     * PRE:    none
     * RETURN: bookings, confirmed, declined, waiting, refunded
     *
     * @return the cards
     */
    public List<Kpi> getKpis()
    {
        OpsLog.Counts c = log.counts();
        String rate = c.bookings() == 0 ? "no bookings yet" : Math.round(100.0 * c.confirmed() / c.bookings()) + "% of bookings";
        return List.of(
                new Kpi("BOOKINGS", String.valueOf(c.bookings()), "since " + HHMM.format(log.since()), "kpi"),
                new Kpi("CONFIRMED", String.valueOf(c.confirmed()), rate, "kpi kpi-good"),
                new Kpi("DECLINED", String.valueOf(c.declined()), "card limit or invalid", "kpi kpi-bad"),
                new Kpi("WAITING FOR PAYMENT", String.valueOf(c.waiting()), isPaused() ? "payments are paused" : "normally 0", "kpi kpi-info"),
                new Kpi("REFUNDED", String.valueOf(c.refunded()), "cancelled trips", "kpi"));
    }

    /**
     * WHAT:   Each service's health.
     * RETURN: the services in display order
     *
     * @return the services
     */
    public List<OpsSnapshot.Service> getServices()
    {
        return monitor.snapshot().services();
    }

    /**
     * WHAT:   The queue rows, or an empty list if RabbitMQ's API cannot be read.
     * RETURN: the rows
     *
     * @return the rows
     */
    public List<QueueRow> getQueues()
    {
        List<QueueInfo> queues = monitor.snapshot().queues();
        if (queues == null) return List.of();
        List<QueueRow> rows = new ArrayList<>();
        for (QueueInfo q : queues)
        {
            boolean dlq = q.name().endsWith(".dlq");
            rows.add(new QueueRow(q.name(), q.messages(), Math.min(100, q.messages() * 10) + "%",
                    dlq ? (q.messages() > 0 ? "queue queue-dlq queue-alert" : "queue queue-dlq") : "queue"));
        }
        return rows;
    }

    /**
     * WHAT:   Whether the queue depths could be read.
     * RETURN: true if RabbitMQ's management API answered
     *
     * @return whether known
     */
    public boolean isQueuesKnown()
    {
        return monitor.snapshot().queues() != null;
    }

    /**
     * WHAT:   The live feed, newest first.
     * RETURN: the entries
     *
     * @return the feed
     */
    public List<OpsLog.Entry> getFeed()
    {
        return log.feed();
    }

    /**
     * WHAT:   The median saga time.
     * RETURN: e.g. "74 ms", or "–" with no samples
     *
     * @return the median
     */
    public String getP50()
    {
        long v = OpsLog.percentile(log.samples(), 50);
        return v < 0 ? "–" : v + " ms";
    }

    /**
     * WHAT:   The 95th-percentile saga time.
     * RETURN: e.g. "120 ms", or "–" with no samples
     *
     * @return the p95
     */
    public String getP95()
    {
        long v = OpsLog.percentile(log.samples(), 95);
        return v < 0 ? "–" : v + " ms";
    }

    /**
     * WHAT:   The most recent saga times as bars, scaled to the largest.
     * RETURN: up to BARS bars, oldest first
     *
     * @return the bars
     */
    public List<Bar> getBars()
    {
        List<Long> all = log.samples();
        List<Long> recent = all.subList(Math.max(0, all.size() - BARS), all.size());
        long max = recent.stream().mapToLong(Long::longValue).max().orElse(1);
        return recent.stream().map(ms -> new Bar(ms, Math.max(4, Math.round(100.0 * ms / Math.max(1, max))) + "%")).toList();
    }

    /**
     * WHAT:   Whether payments are paused.
     * RETURN: true if the switch says so
     *
     * @return whether paused
     */
    public boolean isPaused()
    {
        return Boolean.TRUE.equals(monitor.snapshot().paymentsPaused());
    }

    /**
     * WHAT:   Whether the "break it" controls are available.
     * RETURN: true when demo controls are on
     *
     * @return whether available
     */
    public boolean isDemoControls()
    {
        return events.isDemoControls();
    }

    /**
     * WHAT:   Whether the website is receiving live events.
     * RETURN: true while connected to RabbitMQ
     *
     * @return whether live
     */
    public boolean isLive()
    {
        return events.isConnected();
    }

    /**
     * WHAT:   The status line under the controls.
     * RETURN: the message, or null
     *
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * WHAT:   The status line's classes.
     * RETURN: "msg msg-bad" or "msg"
     *
     * @return the classes
     */
    public String getMessageCss()
    {
        return messageBad ? "msg msg-bad" : "msg";
    }
}
