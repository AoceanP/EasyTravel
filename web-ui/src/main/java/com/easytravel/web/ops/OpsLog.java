package com.easytravel.web.ops;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WHAT:   What the Ops room knows from the events the website has heard since
 *         it started: a feed of recent events, counters, and how long each
 *         booking took from "created" to "payment result".
 * WHY:    Every number on the page is measured, not invented. The timings are
 *         taken where the website hears the two events, so they include both
 *         trips through RabbitMQ: the honest end-to-end saga time.
 *         Everything is bounded (the feed, the samples, the open bookings),
 *         so memory stays flat however long it runs.
 *
 * @author AoceanP
 */
@ApplicationScoped
public class OpsLog
{
    /** WHAT: events kept for the feed. */
    static final int FEED_SIZE = 14;

    /** WHAT: timing samples kept. */
    static final int SAMPLES = 100;

    /** WHAT: bookings remembered while waiting for their payment result. */
    private static final int OPEN_LIMIT = 500;

    /** WHAT: how a feed time is shown, e.g. "17:38:44.491". */
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    /**
     * WHAT:   One line in the feed.
     *
     * @param time   when the website heard it
     * @param name   what happened, e.g. "PaymentEvent"
     * @param detail the booking and outcome
     * @param css    its colour class
     */
    public record Entry(String time, String name, String detail, String css)
    {
    }

    /**
     * WHAT:   The counters.
     *
     * @param bookings  bookings created
     * @param confirmed payments that succeeded
     * @param declined  payments that were declined
     * @param waiting   bookings still waiting for a payment result
     * @param refunded  refunds completed
     */
    public record Counts(long bookings, long confirmed, long declined, long waiting, long refunded)
    {
    }

    /** WHAT: the feed, newest first. */
    private final Deque<Entry> feed = new ArrayDeque<>();

    /** WHAT: created-to-paid times in milliseconds, oldest first. */
    private final Deque<Long> samples = new ArrayDeque<>();

    /** WHAT: when each still-open booking was created; insertion-ordered so the oldest can be dropped. */
    private final Map<String, Instant> open = new LinkedHashMap<>();

    /** WHAT: bookings whose payment result has been heard, to spot duplicates. */
    private final Map<String, Boolean> decided = new LinkedHashMap<>();

    private long bookings, confirmed, declined, refunded;

    /** WHAT: when the website started listening. */
    private final Instant since;

    /** WHAT: the time source; replaceable in tests. */
    private final Clock clock;

    /**
     * WHAT:   Creates the log with the system clock.
     * PRE:    none
     */
    public OpsLog()
    {
        this(Clock.systemUTC());
    }

    /**
     * WHAT:   Creates the log with a given clock.
     * PRE:    clock is not null
     *
     * @param clock the time source
     */
    OpsLog(Clock clock)
    {
        this.clock = clock;
        this.since = clock.instant();
    }

    /**
     * WHAT:   Records a new booking.
     * PRE:    bookingId is not null
     * POST:   counted, timed from now, and in the feed
     *
     * @param bookingId the booking
     * @param amount    its price, formatted
     */
    public synchronized void bookingCreated(String bookingId, String amount)
    {
        Instant now = clock.instant();
        bookings++;
        open.put(bookingId, now);
        if (open.size() > OPEN_LIMIT)
        {
            open.remove(open.keySet().iterator().next());
        }
        add(now, "BookingCreated", shortId(bookingId) + " · " + amount + " → payment", "ev-info");
    }

    /**
     * WHAT:   Records a payment result, or flags it as a duplicate.
     * WHY:    A duplicate is shown as such (booking-service ignores it) and is
     *         not counted twice.
     * PRE:    bookingId is not null
     * POST:   counted and timed (unless a duplicate), and in the feed
     *
     * @param bookingId the booking
     * @param success   whether it was charged
     * @param reason    why not, or null
     */
    public synchronized void paymentResult(String bookingId, boolean success, String reason)
    {
        Instant now = clock.instant();
        if (decided.containsKey(bookingId))
        {
            add(now, "PaymentEvent (copy)", shortId(bookingId) + " · already decided, ignored", "ev-warn");
            return;
        }
        decided.put(bookingId, success);
        if (decided.size() > OPEN_LIMIT)
        {
            decided.remove(decided.keySet().iterator().next());
        }
        if (success) confirmed++; else declined++;
        Instant created = open.remove(bookingId);
        String took = "";
        if (created != null)
        {
            long ms = Duration.between(created, now).toMillis();
            samples.addLast(ms);
            if (samples.size() > SAMPLES) samples.removeFirst();
            took = " in " + ms + " ms";
        }
        add(now, "PaymentEvent", shortId(bookingId) + (success ? " · charged" : " · declined: " + reason) + took,
                success ? "ev-good" : "ev-bad");
    }

    /**
     * WHAT:   Records a refund request (a customer cancelled).
     * PRE:    bookingId is not null
     * POST:   in the feed
     *
     * @param bookingId the booking
     */
    public synchronized void refundRequested(String bookingId)
    {
        add(clock.instant(), "RefundRequested", shortId(bookingId) + " · customer cancelled", "ev-info");
    }

    /**
     * WHAT:   Records a refund result.
     * PRE:    bookingId is not null
     * POST:   counted if it succeeded, and in the feed
     *
     * @param bookingId the booking
     * @param ok        whether the money went back
     * @param reason    why not, or null
     */
    public synchronized void refundResult(String bookingId, boolean ok, String reason)
    {
        if (ok) refunded++;
        add(clock.instant(), "RefundEvent", shortId(bookingId) + (ok ? " · refunded" : " · failed: " + reason),
                ok ? "ev-good" : "ev-bad");
    }

    /**
     * WHAT:   Records something the Ops room did itself (a control was used).
     * PRE:    name and detail are not null
     * POST:   in the feed
     *
     * @param name   what was done
     * @param detail what it means
     */
    public synchronized void control(String name, String detail)
    {
        add(clock.instant(), name, detail, "ev-warn");
    }

    /**
     * WHAT:   The feed, newest first.
     * RETURN: a copy
     *
     * @return the entries
     */
    public synchronized List<Entry> feed()
    {
        return List.copyOf(feed);
    }

    /**
     * WHAT:   The counters now.
     * RETURN: the counts
     *
     * @return the counts
     */
    public synchronized Counts counts()
    {
        return new Counts(bookings, confirmed, declined, Math.max(0, bookings - confirmed - declined), refunded);
    }

    /**
     * WHAT:   The created-to-paid times, oldest first.
     * RETURN: a copy, in milliseconds
     *
     * @return the samples
     */
    public synchronized List<Long> samples()
    {
        return List.copyOf(samples);
    }

    /**
     * WHAT:   When the website started listening.
     * RETURN: the time
     *
     * @return the start time
     */
    public Instant since()
    {
        return since;
    }

    /**
     * WHAT:   The p-th percentile of some samples (nearest-rank method).
     * PRE:    0 < p <= 100
     * RETURN: the value, or -1 when there are no samples
     *
     * @param values the samples
     * @param p      the percentile, e.g. 95
     * @return the percentile
     */
    static long percentile(List<Long> values, double p)
    {
        if (values.isEmpty()) return -1;
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(null);
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.max(0, rank - 1));
    }

    /**
     * WHAT:   Adds a feed line, dropping the oldest beyond FEED_SIZE.
     * PRE:    called while holding the lock
     * POST:   the line is first in the feed
     *
     * @param at     when
     * @param name   what
     * @param detail details
     * @param css    colour class
     */
    private void add(Instant at, String name, String detail, String css)
    {
        feed.addFirst(new Entry(TIME.format(at), name, detail, css));
        while (feed.size() > FEED_SIZE) feed.removeLast();
    }

    /**
     * WHAT:   The first 8 characters of a booking id.
     * PRE:    none
     * RETURN: the short id
     *
     * @param id the id
     * @return the short form
     */
    private static String shortId(String id)
    {
        return id == null ? "?" : id.length() > 8 ? id.substring(0, 8) : id;
    }
}
