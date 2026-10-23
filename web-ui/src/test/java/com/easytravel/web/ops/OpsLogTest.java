package com.easytravel.web.ops;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT:   Tests for the Ops room's counters, timings and feed.
 *
 * @author AoceanP
 */
class OpsLogTest
{
    /**
     * WHAT:   A booking, its payment result, then a copy of that result.
     * WHY:    Counted once, timed from "created", and the copy is labelled, not counted.
     * RETURN: nothing; fails if counts, timing or feed are wrong
     */
    @Test
    void countsTimesAndSpotsDuplicates()
    {
        StepClock clock = new StepClock(Instant.parse("2026-10-23T10:00:00Z"));
        OpsLog log = new OpsLog(clock);

        log.bookingCreated("aaaaaaaa-1111", "210.00");
        clock.now = clock.now.plusMillis(74);
        log.paymentResult("aaaaaaaa-1111", true, null);
        log.paymentResult("aaaaaaaa-1111", true, null);

        assertThat(log.counts()).isEqualTo(new OpsLog.Counts(1, 1, 0, 0, 0));
        assertThat(log.samples()).containsExactly(74L);
        assertThat(log.feed()).extracting(OpsLog.Entry::name)
                .containsExactly("PaymentEvent (copy)", "PaymentEvent", "BookingCreated");
        assertThat(log.feed().get(1).detail()).isEqualTo("aaaaaaaa · charged in 74 ms");
    }

    /**
     * WHAT:   A booking still waiting, and a completed refund.
     * WHY:    "Waiting" is created minus decided; refunds are counted.
     * RETURN: nothing; fails if counts are wrong
     */
    @Test
    void waitingAndRefunds()
    {
        OpsLog log = new OpsLog();
        log.bookingCreated("b-1", "210.00");
        log.bookingCreated("b-2", "525.00");
        log.paymentResult("b-1", false, "card limit");
        log.refundResult("b-9", true, null);
        assertThat(log.counts()).isEqualTo(new OpsLog.Counts(2, 0, 1, 1, 1));
    }

    /**
     * WHAT:   The feed limit.
     * WHY:    Memory stays flat: only the newest FEED_SIZE lines are kept.
     * RETURN: nothing; fails if it grows past the limit
     */
    @Test
    void feedIsBounded()
    {
        OpsLog log = new OpsLog();
        for (int i = 0; i < OpsLog.FEED_SIZE + 10; i++)
        {
            log.control("c" + i, "x");
        }
        assertThat(log.feed()).hasSize(OpsLog.FEED_SIZE);
        assertThat(log.feed().getFirst().name()).isEqualTo("c" + (OpsLog.FEED_SIZE + 9));
    }

    /**
     * WHAT:   Percentiles by the nearest-rank method.
     * RETURN: nothing; fails if a percentile is wrong
     */
    @Test
    void percentiles()
    {
        List<Long> v = List.of(50L, 10L, 40L, 20L, 30L);
        assertThat(OpsLog.percentile(v, 50)).isEqualTo(30);
        assertThat(OpsLog.percentile(v, 95)).isEqualTo(50);
        assertThat(OpsLog.percentile(List.of(), 50)).isEqualTo(-1);
    }

    /**
     * WHAT:   A clock the test moves by hand.
     */
    private static final class StepClock extends Clock
    {
        /** WHAT: the current time. */
        Instant now;

        /**
         * WHAT:   Creates the clock.
         * PRE:    start is not null
         *
         * @param start the starting time
         */
        StepClock(Instant start)
        {
            now = start;
        }

        @Override
        public ZoneOffset getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone)
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
