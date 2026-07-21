package com.easytravel.payment.service;

import com.easytravel.common.dto.PaymentResponseDTO;
import com.easytravel.common.dto.PaymentResponseDTO.PaymentStatus;
import com.easytravel.payment.entity.Payment;
import com.easytravel.payment.gateway.PaymentGateway;
import com.easytravel.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment business logic: process a charge for a booking and record the result.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Processes a payment for a booking.
     *
     * <p>IDEMPOTENCY: if this booking was already paid, we return the existing payment
     * instead of charging again. Never double-charge. This is the single most important
     * property of a payment endpoint.
     *
     * <p>Otherwise we call the (mock) gateway, persist the outcome, and return a
     * PaymentResponseDTO — the shared contract that will become a RabbitMQ event in Phase 3.
     */
    @Transactional
    public PaymentResponseDTO processPayment(String bookingId, BigDecimal amount) {
        // Idempotency guard: already paid? return the prior result.
        var existing = paymentRepository.findByBookingId(bookingId);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        boolean success = paymentGateway.charge(bookingId, amount);

        Payment payment = new Payment(
                UUID.randomUUID().toString(),
                bookingId,
                amount,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                Instant.now()
        );
        Payment saved = paymentRepository.save(payment);
        return toDto(saved);
    }

    /** Entity → DTO translation. The stored entity stays internal; the DTO crosses the wire. */
    private PaymentResponseDTO toDto(Payment p) {
        return new PaymentResponseDTO(
                p.getTransactionId(),
                p.getBookingId(),
                p.getStatus(),
                p.getAmount(),
                p.getProcessedAt()
        );
    }
}