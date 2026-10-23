package com.easytravel.payment.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * WHAT:   A fake card processor that approves everything up to a card limit.
 * WHY:    Lets the whole flow run with no real money and no external
 *         service, while still producing declines on purpose: any charge
 *         over the limit (default 2000.00) is declined, so a FIRST-class
 *         booking on the most expensive flight demonstrates the failure path.
 *
 * @author AoceanP
 */
@Component
public class MockPaymentGateway implements PaymentGateway
{
    /** WHAT: the highest amount the fake card accepts. */
    private final BigDecimal cardLimit;

    /**
     * WHAT:   Creates the gateway.
     * PRE:    cardLimit is positive
     *
     * @param cardLimit the highest accepted amount, from payment.mock.card-limit
     */
    public MockPaymentGateway(@Value("${payment.mock.card-limit:2000.00}") BigDecimal cardLimit)
    {
        this.cardLimit = cardLimit;
    }

    /**
     * WHAT:   Approves the charge unless it is not positive or over the card limit.
     * PRE:    none
     * RETURN: approved, or declined with a reason
     *
     * @param bookingId the booking being paid for
     * @param userId    the customer
     * @param amount    the amount to charge
     * @return the result
     */
    @Override
    public ChargeResult charge(String bookingId, String userId, BigDecimal amount)
    {
        if (amount == null || amount.signum() <= 0)
        {
            return ChargeResult.declined("invalid amount");
        }
        if (amount.compareTo(cardLimit) > 0)
        {
            return ChargeResult.declined("card limit of " + cardLimit + " exceeded");
        }
        return ChargeResult.approved();
    }
}
