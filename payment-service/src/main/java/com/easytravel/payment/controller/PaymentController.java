package com.easytravel.payment.controller;

import com.easytravel.common.dto.PaymentResponseDTO;
import com.easytravel.payment.service.PaymentService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WHAT:   Read-only HTTP access to payments.
 * WHY:    Payments are only CREATED by booking events, never by an HTTP
 *         call: an earlier version accepted "POST /api/payments?amount=..."
 *         from anyone, which let a client pay any amount it liked.
 *
 *         GET /api/payments?bookingId=1
 *
 * @author AoceanP
 */
@RestController
@Validated
public class PaymentController
{
    /** WHAT: does the actual work. */
    private final PaymentService paymentService;

    /**
     * WHAT:   Creates the controller.
     * PRE:    paymentService is not null
     *
     * @param paymentService the payment logic
     */
    public PaymentController(PaymentService paymentService)
    {
        this.paymentService = paymentService;
    }

    /**
     * WHAT:   Returns the payment for a booking.
     * PRE:    bookingId is not blank
     * RETURN: the payment, or 404 if the booking has not been charged
     *
     * @param bookingId the booking
     * @return the payment
     */
    @GetMapping("/api/payments")
    public PaymentResponseDTO getPayment(@RequestParam @NotBlank String bookingId)
    {
        return paymentService.getPaymentForBooking(bookingId);
    }
}
