package com.tut2.group3.emailservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * EmailService Application
 *
 * Provides email notification services for the Store system.
 * Receives messages from DeliveryCo and Store via RabbitMQ and
 * simulates sending emails by printing notification logs.
 */
@SpringBootApplication
public class EmailServiceApplication {

    public static void main(String[] args) {
        // Set default timezone to Australia/Sydney
        TimeZone.setDefault(TimeZone.getTimeZone("Australia/Sydney"));

        SpringApplication.run(EmailServiceApplication.class, args);

        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("EmailService is running on port 8084");
        System.out.println("════════════════════════════════════════════════════════════");
    }
}
