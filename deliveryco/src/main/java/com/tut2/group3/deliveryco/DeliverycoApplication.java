package com.tut2.group3.deliveryco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DeliveryCo Service - Main Application
 *
 * Handles delivery requests from Store service via RabbitMQ.
 * Automatically progresses delivery status and sends notifications.
 */
@SpringBootApplication
@EnableScheduling
public class DeliverycoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliverycoApplication.class, args);
    }
}
