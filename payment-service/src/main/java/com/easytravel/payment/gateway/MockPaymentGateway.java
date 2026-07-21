package com.easytravel.payment.gateway;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

/**
 * Mock gateway: always "succeeds". Stands in for a real processor so we can build the
 * payment flow end-to-end without external dependencies.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public boolean charge(String bookingId, BigDecimal amount) {
        // Simulate a successful external charge.
        return true;
    }
}