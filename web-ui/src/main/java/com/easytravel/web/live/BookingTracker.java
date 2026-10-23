package com.easytravel.web.live;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WHAT:   Remembers, per booking, which saga events the website has seen and
 *         which browser is watching it.
 * WHY:    Events and the HTTP response race each other: RabbitMQ can deliver
 *         "payment requested" before POST /api/bookings has even returned the
 *         booking id. So events are kept whether or not anyone is watching yet,
 *         and whoever starts watching later sees what already happened.
 *         It covers both sagas: booking/payment and cancellation/refund.
 *         Entries are dropped after 15 minutes; this is a live view, not storage.
 *
 * @author AoceanP
 */
@ApplicationScoped
public class BookingTracker
{
    /** WHAT: how long an entry is kept after it last changed. */
    static final Duration KEEP = Duration.ofMinutes(15);

    /**
     * WHAT:   What the website knows about one booking.
     *
     * @param watcher         the push key of the browser watching it, or null
     * @param created         when "payment requested" was seen, or null
     * @param paid            when the payment result was seen, or null
     * @param success         the payment result, meaningful once paid is set
     * @param reason          why payment failed, or null
     * @param refundRequested when "refund requested" was seen, or null
     * @param refunded        when the refund result was seen, or null
     * @param refundOk        the refund result, meaningful once refunded is set
     * @param refundReason    why the refund failed, or null
     * @param touched         when this entry last changed
     */
    public record Track(String watcher, Instant created, Instant paid, boolean success, String reason,
                        Instant refundRequested, Instant refunded, boolean refundOk, String refundReason,
                        Instant touched)
    {
        /**
         * WHAT:   An empty entry.
         * PRE:    now is not null
         * RETURN: a track with nothing seen yet
         *
         * @param now the current time
         * @return the track
         */
        static Track empty(Instant now)
        {
            return new Track(null, null, null, false, null, null, null, false, null, now);
        }
    }

    /** WHAT: the entries, by booking id. */
    private final Map<String, Track> tracks = new ConcurrentHashMap<>();

    /** WHAT: the time source; replaceable in tests. */
    private final Clock clock;

    /**
     * WHAT:   Creates the tracker with the system clock.
     * PRE:    none
     */
    public BookingTracker()
    {
        this(Clock.systemUTC());
    }

    /**
     * WHAT:   Creates the tracker with a given clock.
     * WHY:    Lets tests move time forward to check expiry.
     * PRE:    clock is not null
     *
     * @param clock the time source
     */
    BookingTracker(Clock clock)
    {
        this.clock = clock;
    }

    /**
     * WHAT:   Registers the browser watching a booking.
     * PRE:    both arguments are not null
     * POST:   later events for this booking return this watcher
     *
     * @param bookingId the booking
     * @param watcher   the browser's push key
     */
    public void watch(String bookingId, String watcher)
    {
        update(bookingId, (t, now) -> new Track(watcher, t.created(), t.paid(), t.success(), t.reason(),
                t.refundRequested(), t.refunded(), t.refundOk(), t.refundReason(), now));
    }

    /**
     * WHAT:   Records that payment was requested for a booking.
     * PRE:    bookingId is not null
     * RETURN: the watching browser, if there is one yet
     *
     * @param bookingId the booking
     * @return the watcher to notify
     */
    public Optional<String> created(String bookingId)
    {
        return update(bookingId, (t, now) -> new Track(t.watcher(), now, t.paid(), t.success(), t.reason(),
                t.refundRequested(), t.refunded(), t.refundOk(), t.refundReason(), now));
    }

    /**
     * WHAT:   Records a payment result for a booking.
     * PRE:    bookingId is not null
     * RETURN: the watching browser, if there is one yet
     *
     * @param bookingId the booking
     * @param success   whether the card was charged
     * @param reason    why it failed, or null
     * @return the watcher to notify
     */
    public Optional<String> paid(String bookingId, boolean success, String reason)
    {
        return update(bookingId, (t, now) -> new Track(t.watcher(), t.created(), now, success, reason,
                t.refundRequested(), t.refunded(), t.refundOk(), t.refundReason(), now));
    }

    /**
     * WHAT:   Records that a refund was requested (the customer cancelled).
     * PRE:    bookingId is not null
     * RETURN: the watching browser, if there is one yet
     *
     * @param bookingId the booking
     * @return the watcher to notify
     */
    public Optional<String> refundRequested(String bookingId)
    {
        return update(bookingId, (t, now) -> new Track(t.watcher(), t.created(), t.paid(), t.success(), t.reason(),
                now, t.refunded(), t.refundOk(), t.refundReason(), now));
    }

    /**
     * WHAT:   Records a refund result.
     * PRE:    bookingId is not null
     * RETURN: the watching browser, if there is one yet
     *
     * @param bookingId the booking
     * @param ok        whether the money went back
     * @param reason    why it failed, or null
     * @return the watcher to notify
     */
    public Optional<String> refunded(String bookingId, boolean ok, String reason)
    {
        return update(bookingId, (t, now) -> new Track(t.watcher(), t.created(), t.paid(), t.success(), t.reason(),
                t.refundRequested(), now, ok, reason, now));
    }

    /**
     * WHAT:   What is known about a booking.
     * PRE:    none
     * RETURN: the entry, or empty if none (or it expired)
     *
     * @param bookingId the booking
     * @return the entry
     */
    public Optional<Track> get(String bookingId)
    {
        return bookingId == null ? Optional.empty() : Optional.ofNullable(tracks.get(bookingId));
    }

    /**
     * WHAT:   How one change is applied to an entry.
     */
    @FunctionalInterface
    private interface Change
    {
        /**
         * WHAT:   Builds the new entry from the old one.
         * PRE:    both arguments are not null
         * RETURN: the new entry
         *
         * @param old the current entry (an empty one if there was none)
         * @param now the current time
         * @return the new entry
         */
        Track apply(Track old, Instant now);
    }

    /**
     * WHAT:   Applies a change atomically, then drops expired entries.
     * PRE:    bookingId and change are not null
     * POST:   the entry is updated
     * RETURN: the watcher of the updated entry, if any
     *
     * @param bookingId the booking
     * @param change    the change
     * @return the watcher to notify
     */
    private Optional<String> update(String bookingId, Change change)
    {
        Instant now = clock.instant();
        Track t = tracks.compute(bookingId, (id, old) -> change.apply(old == null ? Track.empty(now) : old, now));
        evictOld(now);
        return Optional.ofNullable(t.watcher());
    }

    /**
     * WHAT:   Drops entries that have not changed for KEEP.
     * WHY:    Bounded memory without a background job.
     * PRE:    now is the current time
     * POST:   no entry older than KEEP remains
     *
     * @param now the current time
     */
    private void evictOld(Instant now)
    {
        Instant cutoff = now.minus(KEEP);
        tracks.values().removeIf(t -> t.touched().isBefore(cutoff));
    }
}
