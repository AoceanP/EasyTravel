package com.easytravel.payment.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT:   Tests for the fake card processor's decline rules.
 * WHY:    The demo's failure path depends on the limit being exact.
 *
 * @author AoceanP
 */
class MockPaymentGatewayTest
{
    /** WHAT: a gateway with a 2000.00 limit. */
    private final MockPaymentGateway gateway = new MockPaymentGateway(new BigDecimal("2000.00"));

    /**
     * WHAT:   Exactly the limit, and one cent over.
     * WHY:    The limit itself is allowed; anything above is declined.
     * PRE:    none
     * RETURN: nothing; fails if the boundary is wrong
     */
    @Test
    void limitBoundary()
    {
        assertThat(gateway.charge("1", "alice", new BigDecimal("2000.00")).success()).isTrue();
        assertThat(gateway.charge("1", "alice", new BigDecimal("2000.01")).success()).isFalse();
    }

    /**
     * WHAT:   Zero, negative and missing amounts.
     * WHY:    A fake card should never "charge" a refund or nothing.
     * PRE:    none
     * RETURN: nothing; fails if any is approved
     */
    @Test
    void nonPositiveAmountsAreDeclined()
    {
        assertThat(gateway.charge("1", "alice", BigDecimal.ZERO).success()).isFalse();
        assertThat(gateway.charge("1", "alice", new BigDecimal("-5")).success()).isFalse();
        assertThat(gateway.charge("1", "alice", null).reason()).isEqualTo("invalid amount");
    }
}
