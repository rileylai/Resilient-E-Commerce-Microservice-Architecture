package com.tut2.group3.deliveryco.service;

import com.tut2.group3.deliveryco.dto.DeliveryRequestDTO;
import com.tut2.group3.deliveryco.entity.Delivery;
import com.tut2.group3.deliveryco.entity.enums.DeliveryStatus;

import java.util.List;

/**
 * Delivery Service Interface
 *
 * Provides core business logic for managing deliveries:
 * - Creating delivery records from requests
 * - Updating delivery status
 * - Querying delivery information
 * - Sending status notifications
 */
public interface DeliveryService {

    /**
     * Create a new delivery from a delivery request
     * This is called when Store sends a delivery request after successful payment
     *
     * @param request Delivery request from Store service
     * @return Created Delivery entity
     */
    Delivery createDelivery(DeliveryRequestDTO request);

    /**
     * Update the status of a delivery
     * This triggers notifications to Store and EmailService
     *
     * @param deliveryId ID of the delivery to update
     * @param newStatus New delivery status
     * @return Updated Delivery entity
     */
    Delivery updateDeliveryStatus(Long deliveryId, DeliveryStatus newStatus);

    /**
     * Get delivery information by order ID
     *
     * @param orderId Order ID from Store service
     * @return Delivery entity, or null if not found
     */
    Delivery getDeliveryByOrderId(Long orderId);

    /**
     * Get all deliveries for a specific customer
     *
     * @param customerId Customer ID
     * @return List of deliveries for the customer
     */
    List<Delivery> getDeliveriesByCustomerId(Long customerId);

    /**
     * Get all deliveries with a specific status
     * Used by scheduler to find deliveries that need status progression
     *
     * @param status Delivery status to filter by
     * @return List of deliveries with the specified status
     */
    List<Delivery> getDeliveriesByStatus(DeliveryStatus status);

    /**
     * Get delivery by ID
     *
     * @param deliveryId Delivery ID
     * @return Delivery entity, or null if not found
     */
    Delivery getDeliveryById(Long deliveryId);

    /**
     * Get all deliveries (for admin purposes)
     *
     * @return List of all deliveries
     */
    List<Delivery> getAllDeliveries();

    /**
     * Process delivery status progression
     * This is called by the scheduler to automatically advance delivery status
     * Includes 5% package loss simulation
     *
     * @param delivery Delivery to process
     */
    void processDeliveryStatusProgression(Delivery delivery);

    /**
     * Determine the next status for a delivery
     * Includes 5% package loss rate at the final stage
     *
     * @param currentStatus Current delivery status
     * @return Next delivery status
     */
    DeliveryStatus determineNextStatus(DeliveryStatus currentStatus);
}
