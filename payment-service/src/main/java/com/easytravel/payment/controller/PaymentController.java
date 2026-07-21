package com.easytravel.payment.controller;

import com.easytravel.common.dto.PaymentResponseDTO;
import com.easytravel.payment.service.PaymentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * HTTP edge for payments. Kept minimal: take a bookingId + amount, process, return the
 * PaymentResponseDTO. (In Phase 3 the trigger for this becomes part of the booking flow.)
 */
@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Example: POST /api/payments?bookingId=1&amount=210.00 */
    @PostMapping("/api/payments")
    public PaymentResponseDTO pay(
            @RequestParam String bookingId,
            @RequestParam BigDecimal amount) {
        return paymentService.processPayment(bookingId, amount);
    }
}