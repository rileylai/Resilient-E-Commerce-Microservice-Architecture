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
public class StockReservationResponse {
    private String orderId;
    private String reservationId;
    private List<ReservationDetail> reservations;
    private LocalDateTime timestamp;
}
