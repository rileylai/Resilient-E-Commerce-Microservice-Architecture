package com.tut2.group3.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmReservationResponse {
    private String orderId;
    private String reservationId;
    private String status;
    private List<ReservationDetail> warehouses;
    private LocalDateTime timestamp;
}
