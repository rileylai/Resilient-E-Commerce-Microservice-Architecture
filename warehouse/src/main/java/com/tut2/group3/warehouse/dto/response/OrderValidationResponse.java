package com.tut2.group3.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderValidationResponse {
    private String orderId;
    private Boolean valid;
    private String validationCode; // SUCCESS, INSUFFICIENT_STOCK, PRODUCT_NOT_FOUND
    private String message;
    private List<ProductValidationResult> productResults;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductValidationResult {
        private Long productId;
        private String productName;
        private Integer requestedQuantity;
        private Integer availableQuantity;
        private Boolean available;
        private String reason; // null if available, otherwise reason for unavailability
    }
}
