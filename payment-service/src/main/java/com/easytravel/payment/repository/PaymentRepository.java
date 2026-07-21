package com.easytravel.payment.repository;

import com.easytravel.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository for payments. The derived query findByBookingId lets us check
 * "has this booking already been paid?" — the basis for idempotency.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(String bookingId);
}