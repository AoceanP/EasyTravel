package com.easytravel.web.live;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT:   Tests for the event tracker.
 * WHY:    It must handle events that arrive before anyone watches, and must
 *         not grow forever.
 *
 * @author AoceanP
 */
class BookingTrackerTest
{
    /**
     * WHAT:   An event that beats the HTTP response.
     * WHY:    Nobody to notify yet, but the event is kept for the page to read.
     * RETURN: nothing; fails if the early event is lost
     */
    @Test
    void earlyEventIsKept()
    {
        BookingTracker tracker = new BookingTracker();
        assertThat(tracker.created("b-1")).isEmpty();
        tracker.watch("b-1", "browser-1");
        assertThat(tracker.get("b-1")).get().satisfies(t -> {
            assertThat(t.watcher()).isEqualTo("browser-1");
            assertThat(t.created()).isNotNull();
        });
    }

    /**
     * WHAT:   Events after someone watches.
     * WHY:    Each returns the watcher to notify, and the result is recorded.
     * RETURN: nothing; fails if the watcher or result is wrong
     */
    @Test
    void laterEventsNotifyTheWatcher()
    {
        BookingTracker tracker = new BookingTracker();
        tracker.watch("b-2", "browser-2");
        assertThat(tracker.created("b-2")).contains("browser-2");
        assertThat(tracker.paid("b-2", false, "card limit")).contains("browser-2");
        assertThat(tracker.get("b-2")).get().satisfies(t -> {
            assertThat(t.success()).isFalse();
            assertThat(t.reason()).isEqualTo("card limit");
        });
    }

    /**
     * WHAT:   The cancellation saga's events.
     * WHY:    They are recorded beside the payment ones without losing them.
     * RETURN: nothing; fails if any field is lost
     */
    @Test
    void refundEventsAreRecorded()
    {
        BookingTracker tracker = new BookingTracker();
        tracker.paid("b-3", true, null);
        tracker.watch("b-3", "browser-3");
        assertThat(tracker.refundRequested("b-3")).contains("browser-3");
        assertThat(tracker.refunded("b-3", true, null)).contains("browser-3");
        assertThat(tracker.get("b-3")).get().satisfies(t -> {
            assertThat(t.paid()).isNotNull();
            assertThat(t.success()).isTrue();
            assertThat(t.refundRequested()).isNotNull();
            assertThat(t.refunded()).isNotNull();
            assertThat(t.refundOk()).isTrue();
        });
    }

    /**
     * WHAT:   Entries older than the keep time.
     * WHY:    They are dropped on the next write, so memory stays bounded.
     * RETURN: nothing; fails if an old entry survives
     */
    @Test
    void oldEntriesExpire()
    {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-23T10:00:00Z"));
        BookingTracker tracker = new BookingTracker(clock);
        tracker.watch("old", "browser");
        clock.now = clock.now.plus(BookingTracker.KEEP).plusSeconds(1);
        tracker.watch("new", "browser");
        assertThat(tracker.get("old")).isEmpty();
        assertThat(tracker.get("new")).isPresent();
        assertThat(tracker.get(null)).isEmpty();
    }

    /**
     * WHAT:   A clock the test can move forward.
     */
    private static final class MutableClock extends Clock
    {
        /** WHAT: the current time. */
        Instant now;

        /**
         * WHAT:   Creates the clock.
         * PRE:    start is not null
         *
         * @param start the starting time
         */
        MutableClock(Instant start)
        {
            this.now = start;
        }

        @Override
        public ZoneOffset getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return now;
        }
    }
}
