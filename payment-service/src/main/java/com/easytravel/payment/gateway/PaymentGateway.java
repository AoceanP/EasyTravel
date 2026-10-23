package com.easytravel.payment.gateway;

import java.math.BigDecimal;

/**
 * WHAT:   An external card processor (Stripe, Adyen, ...).
 * WHY:    PaymentService depends on this interface, so a real processor can
 *         replace the mock without changing the service.
 *
 * @author AoceanP
 */
public interface PaymentGateway
{
    /**
     * WHAT:   Charges a customer for a booking.
     * PRE:    amount is positive
     * RETURN: whether it succeeded, and why not if it failed
     *
     * @param bookingId the booking being paid for
     * @param userId    the customer
     * @param amount    the amount to charge
     * @return the result
     */
    ChargeResult charge(String bookingId, String userId, BigDecimal amount);

    /**
     * WHAT:   Returns a charge to the customer's card.
     * PRE:    transactionId is a successful charge; amount is what it charged
     * RETURN: whether the refund went through, and why not if it failed
     *
     * @param bookingId     the booking being cancelled
     * @param transactionId the charge to refund
     * @param amount        the amount to return
     * @return the result
     */
    ChargeResult refund(String bookingId, String transactionId, BigDecimal amount);

    /**
     * WHAT:   The outcome of one charge.
     * WHY:    A plain boolean cannot say WHY a card was declined.
     *
     * @param success whether the charge went through
     * @param reason  why it was declined, or null on success
     */
    record ChargeResult(boolean success, String reason)
    {
        /**
         * WHAT:   A successful charge.
         * RETURN: the result
         *
         * @return the result
         */
        public static ChargeResult approved()
        {
            return new ChargeResult(true, null);
        }

        /**
         * WHAT:   A declined charge.
         * PRE:    reason is not blank
         * RETURN: the result
         *
         * @param reason why it was declined
         * @return the result
         */
        public static ChargeResult declined(String reason)
        {
            return new ChargeResult(false, reason);
        }
    }
}
