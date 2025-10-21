package com.tut2.group3.emailservice.listener;

import com.tut2.group3.emailservice.dto.DeliveryStatusUpdateDTO;
import com.tut2.group3.emailservice.dto.OrderFailureNotificationDTO;
import com.tut2.group3.emailservice.dto.RefundNotificationDTO;
import com.tut2.group3.emailservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Email Notification Listener
 *
 * Listens to multiple queues for email notification requests
 * from DeliveryCo and Store services.
 */
@Component
public class EmailNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

    @Autowired
    private EmailService emailService;

    /**
     * Listen for delivery status update notifications from DeliveryCo
     *
     * Queue: delivery.email.queue
     * Routing Key: notification.email
     * Message Type: DeliveryStatusUpdateDTO
     */
    @RabbitListener(queues = "delivery.email.queue")
    public void handleDeliveryStatusNotification(DeliveryStatusUpdateDTO statusUpdate) {
        log.info("════════════════════════════════════════════════════════════");
        log.info("Received delivery status notification from RabbitMQ");
        log.info("  Order ID: {}", statusUpdate.getOrderId());
        log.info("  Status: {}", statusUpdate.getNewStatus());
        log.info("  Customer Email: {}", statusUpdate.getCustomerEmail());
        log.info("  Timestamp: {}", statusUpdate.getTimestamp());
        log.info("════════════════════════════════════════════════════════════");

        try {
            // Only send emails for specific statuses (as per requirements)
            // Requirements: notify when picked up, in transit, and delivered
            if (shouldSendEmail(statusUpdate.getNewStatus())) {
                emailService.sendDeliveryStatusNotification(statusUpdate);
            } else {
                log.info("Skipping email notification for status: {} (not required by business rules)",
                        statusUpdate.getNewStatus());
            }

        } catch (Exception e) {
            log.error("Error processing delivery status notification for order {}: {}",
                    statusUpdate.getOrderId(), e.getMessage(), e);
            // In production, consider implementing retry logic or dead letter queue
        }
    }

    /**
     * Determine if email should be sent for this status
     * Based on PDF requirements (page 1-2):
     * - PICKED_UP: Send email (goods now in depot)
     * - IN_TRANSIT: Send email (on delivery truck)
     * - DELIVERED: Send email (delivery complete)
     * - LOST: Send email (package lost)
     * - REQUEST_RECEIVED: Do NOT send (internal status)
     *
     * @param status Delivery status
     * @return true if email should be sent
     */
    private boolean shouldSendEmail(String status) {
        return switch (status) {
            case "PICKED_UP", "IN_TRANSIT", "DELIVERED", "LOST" -> true;
            case "REQUEST_RECEIVED" -> false;
            default -> {
                log.warn("Unknown delivery status: {}", status);
                yield false;
            }
        };
    }

    /**
     * Listen for order failure notifications from Store
     *
     * Queue: email.orderfail.queue
     * Routing Key: email.orderfail
     * Message Type: OrderFailureNotificationDTO
     */
    @RabbitListener(queues = "email.orderfail.queue")
    public void handleOrderFailureNotification(OrderFailureNotificationDTO failureNotification) {
        log.info("════════════════════════════════════════════════════════════");
        log.info("Received order failure notification from RabbitMQ");
        log.info("  Order ID: {}", failureNotification.getOrderId());
        log.info("  Customer Email: {}", failureNotification.getCustomerEmail());
        log.info("  Reason: {}", failureNotification.getReason());
        log.info("  Timestamp: {}", failureNotification.getTimestamp());
        log.info("════════════════════════════════════════════════════════════");

        try {
            emailService.sendOrderFailureNotification(
                    failureNotification.getCustomerEmail(),
                    failureNotification.getOrderId(),
                    failureNotification.getReason()
            );
        } catch (Exception e) {
            log.error("Error processing order failure notification for order {}: {}",
                    failureNotification.getOrderId(), e.getMessage(), e);
            // In production, consider implementing retry logic or dead letter queue
        }
    }

    /**
     * Listen for refund notifications from Store
     *
     * Queue: email.refund.queue
     * Routing Key: email.refund
     * Message Type: RefundNotificationDTO
     */
    @RabbitListener(queues = "email.refund.queue")
    public void handleRefundNotification(RefundNotificationDTO refundNotification) {
        log.info("════════════════════════════════════════════════════════════");
        log.info("Received refund notification from RabbitMQ");
        log.info("  Order ID: {}", refundNotification.getOrderId());
        log.info("  Customer Email: {}", refundNotification.getCustomerEmail());
        log.info("  Refund Amount: ${}", refundNotification.getAmount());
        log.info("  Reason: {}", refundNotification.getReason());
        log.info("  Timestamp: {}", refundNotification.getTimestamp());
        log.info("════════════════════════════════════════════════════════════");

        try {
            emailService.sendRefundNotification(
                    refundNotification.getCustomerEmail(),
                    refundNotification.getOrderId(),
                    refundNotification.getAmount()
            );
        } catch (Exception e) {
            log.error("Error processing refund notification for order {}: {}",
                    refundNotification.getOrderId(), e.getMessage(), e);
            // In production, consider implementing retry logic or dead letter queue
        }
    }
}
