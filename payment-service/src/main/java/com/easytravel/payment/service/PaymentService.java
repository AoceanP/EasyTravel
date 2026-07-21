package com.easytravel.payment.service;

import com.easytravel.common.dto.PaymentResponseDTO;
import com.easytravel.common.dto.PaymentResponseDTO.PaymentStatus;
import com.easytravel.common.event.MessagingConstants;
import com.easytravel.common.event.PaymentEvent;
import com.easytravel.payment.entity.Payment;
import com.easytravel.payment.gateway.PaymentGateway;
import com.easytravel.payment.repository.PaymentRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final RabbitTemplate rabbitTemplate;   // NEW: our publisher

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          RabbitTemplate rabbitTemplate) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public PaymentResponseDTO processPayment(String bookingId, BigDecimal amount) {
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

        // NEW: on success, PUBLISH an event and forget. We don't call booking-service
        // directly — we announce "payment happened" to the exchange. Whoever cares
        // (booking-service) will react. Payment has no knowledge of the consumer.
        if (success) {
            PaymentEvent event = new PaymentEvent(
                    saved.getBookingId(),
                    saved.getTransactionId(),
                    true,
                    saved.getAmount()
            );
            rabbitTemplate.convertAndSend(
                    MessagingConstants.PAYMENT_EXCHANGE,
                    MessagingConstants.PAYMENT_ROUTING_KEY,
                    event
            );
        }

        return toDto(saved);
    }

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