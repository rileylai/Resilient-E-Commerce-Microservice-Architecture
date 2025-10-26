package com.tut2.group3.store.dto.deliveryco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryDto {
    private Long id;
    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private String warehouseIds;
    private String status; // REQUEST_RECEIVED, PICKED_UP, IN_TRANSIT, DELIVERED, LOST
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

