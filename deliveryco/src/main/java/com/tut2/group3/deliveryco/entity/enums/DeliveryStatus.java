package com.tut2.group3.deliveryco.entity.enums;

/**
 * Delivery status enum representing the state machine for deliveries
 */
public enum DeliveryStatus {
    /**
     * Initial state: Delivery request has been received
     */
    REQUEST_RECEIVED,

    /**
     * Items have been picked up from the warehouse(s)
     */
    PICKED_UP,

    /**
     * Package is in transit to the customer
     */
    IN_TRANSIT,

    /**
     * Package has been successfully delivered to the customer
     */
    DELIVERED,

    /**
     * Package has been lost (simulates 5% failure rate)
     */
    LOST
}