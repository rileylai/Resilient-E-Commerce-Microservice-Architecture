package com.tut2.group3.store.dto.deliveryco;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryStatusRequest {
    private String status; // REQUEST_RECEIVED, PICKED_UP, IN_TRANSIT, DELIVERED, LOST
}

