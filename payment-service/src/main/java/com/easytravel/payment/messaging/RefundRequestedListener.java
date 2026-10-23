package com.easytravel.payment.messaging;

import com.easytravel.common.event.MessagingConstants;
import com.easytravel.common.event.RefundRequestedEvent;
import com.easytravel.payment.service.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * WHAT:   Receives refund requests from RabbitMQ and refunds them.
 * WHY:    Refunds start only from this event, sent by booking-service after a
 *         customer cancels: there is no public "refund" endpoint to abuse.
 *
 * @author AoceanP
 */
@Component
public class RefundRequestedListener
{
    /** WHAT: does the actual work. */
    private final PaymentService paymentService;

    /**
     * WHAT:   Creates the listener.
     * PRE:    paymentService is not null
     *
     * @param paymentService the payment logic
     */
    public RefundRequestedListener(PaymentService paymentService)
    {
        this.paymentService = paymentService;
    }

    /**
     * WHAT:   Handles one refund request.
     * PRE:    called by Spring for each message on the refund-requested queue
     * POST:   the payment is refunded at most once, however often the message arrives
     *
     * @param event the refund request
     */
    @RabbitListener(queues = MessagingConstants.REFUND_REQUESTED_QUEUE)
    public void onRefundRequested(RefundRequestedEvent event)
    {
        paymentService.processRefund(event);
    }
}
