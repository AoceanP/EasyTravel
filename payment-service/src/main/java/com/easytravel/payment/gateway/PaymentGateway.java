package com.easytravel.payment.gateway;

import java.math.BigDecimal;

/**
 * Abstraction over an external payment processor (Stripe, Adyen, etc.). The service
 * depends on THIS, so a real gateway can replace the mock with no ripple.
 */
public interface PaymentGateway {
    /** @return true if the charge succeeded. */
    boolean charge(String bookingId, BigDecimal amount);
}