-- Drop existing table to ensure clean state
DROP TABLE IF EXISTS deliveries;

-- Create deliveries table
CREATE TABLE deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Unique identifier',
    order_id BIGINT NOT NULL COMMENT 'Associated order ID from store service',
    customer_id BIGINT NOT NULL COMMENT 'Customer ID',
    customer_email VARCHAR(255) COMMENT 'Customer email for sending notifications',
    warehouse_ids VARCHAR(255) COMMENT 'Comma-separated warehouse IDs (e.g. "1,3")',
    status VARCHAR(50) NOT NULL COMMENT 'Delivery status',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    UNIQUE KEY uk_order_id (order_id) COMMENT 'Prevent duplicate deliveries for same order',
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Delivery information table';