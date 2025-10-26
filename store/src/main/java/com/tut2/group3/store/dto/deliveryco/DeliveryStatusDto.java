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
public class DeliveryStatusDto {
    private Long deliveryId;
    private Long orderId;
    private String status;
    private LocalDateTime updatedAt;
}

