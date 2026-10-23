package com.easytravel.payment.repository;

import com.easytravel.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * WHAT:   Database access for payments.
 * WHY:    findByBookingId answers "has this booking already been paid?",
 *         the first line of defence against charging twice.
 *
 * @author AoceanP
 */
public interface PaymentRepository extends JpaRepository<Payment, Long>
{
    /**
     * WHAT:   The payment for a booking, if there is one.
     * PRE:    bookingId is not null
     * RETURN: the payment, or empty
     *
     * @param bookingId the booking
     * @return the payment
     */
    Optional<Payment> findByBookingId(String bookingId);
}
