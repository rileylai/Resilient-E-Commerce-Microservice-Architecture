package com.tut2.group3.emailservice.service;

import com.tut2.group3.emailservice.dto.DeliveryStatusUpdateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Email Service
 * Simulates sending email notifications by printing formatted logs.
 * In a production environment, this would integrate with an actual email provider (SMTP, SendGrid, etc.)
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Send delivery status notification email
     *
     * @param statusUpdate Delivery status update information
     */
    public void sendDeliveryStatusNotification(DeliveryStatusUpdateDTO statusUpdate) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String subject = getEmailSubject(statusUpdate.getNewStatus());
        String emailBody = statusUpdate.getMessage();

        // Simulate sending email by printing formatted log
        printEmailNotification(
                timestamp,
                statusUpdate.getCustomerEmail(),
                statusUpdate.getOrderId(),
                subject,
                emailBody,
                statusUpdate.getNewStatus()
        );

        log.info("Email notification sent to {} for order {} (Status: {})",
                statusUpdate.getCustomerEmail(), statusUpdate.getOrderId(), statusUpdate.getNewStatus());
    }

    /**
     * Get email subject based on delivery status
     *
     * @param status Delivery status
     * @return Email subject
     */
    private String getEmailSubject(String status) {
        return switch (status) {
            case "REQUEST_RECEIVED" -> "Delivery Request Received";
            case "PICKED_UP" -> "Your Package Has Been Picked Up";
            case "IN_TRANSIT" -> "Your Package Is On The Way";
            case "DELIVERED" -> "Your Package Has Been Delivered";
            case "LOST" -> "Package Delivery Issue";
            default -> "Order Status Update";
        };
    }

    /**
     * Print formatted email notification
     *
     * @param timestamp Timestamp
     * @param recipient Recipient email
     * @param orderId Order ID
     * @param subject Email subject
     * @param body Email body
     * @param status Delivery status
     */
    private void printEmailNotification(String timestamp, String recipient, Long orderId,
                                        String subject, String body, String status) {
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("EMAIL NOTIFICATION SERVICE");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Time:       " + timestamp);
        System.out.println("Recipient:  " + recipient);
        System.out.println("Order ID:   " + orderId);
        System.out.println("Status:     " + status);
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Subject:    " + subject);
        System.out.println("Body:       " + body);
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Email sent successfully");
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println();
    }

    /**
     * Send order failure notification
     *
     * @param customerEmail Customer email
     * @param orderId Order ID
     * @param reason Failure reason
     */
    public void sendOrderFailureNotification(String customerEmail, Long orderId, String reason) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("EMAIL NOTIFICATION SERVICE - Order Failure");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Time:       " + timestamp);
        System.out.println("Recipient:  " + customerEmail);
        System.out.println("Order ID:   " + orderId);
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Subject:    Order Processing Failed");
        System.out.println("Body:       We apologize, but your order #" + orderId + " has failed.");
        System.out.println("            Reason: " + reason);
        System.out.println("            Your order has been cancelled. Any charges will be refunded.");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Email sent successfully");
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println();

        log.info("Order failure notification sent to {} for order {}", customerEmail, orderId);
    }

    /**
     * Send refund notification
     *
     * @param customerEmail Customer email
     * @param orderId Order ID
     * @param amount Refund amount
     */
    public void sendRefundNotification(String customerEmail, Long orderId, Double amount) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("EMAIL NOTIFICATION SERVICE - Refund");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Time:       " + timestamp);
        System.out.println("Recipient:  " + customerEmail);
        System.out.println("Order ID:   " + orderId);
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Subject:    Refund Processed Successfully");
        System.out.println("Body:       Your order #" + orderId + " has been refunded.");
        System.out.println("            Refund amount: $" + String.format("%.2f", amount));
        System.out.println("            The refund will appear in your account within 3-5 business days.");
        System.out.println("────────────────────────────────────────────────────────────");
        System.out.println("Email sent successfully");
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println();

        log.info("Refund notification sent to {} for order {} (Amount: ${})", customerEmail, orderId, amount);
    }
}
