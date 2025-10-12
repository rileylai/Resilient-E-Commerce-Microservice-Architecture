-- Create deliveries table if it doesn't exist
CREATE TABLE IF NOT EXISTS deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '唯一标识符',
    order_id BIGINT NOT NULL COMMENT '关联的store服务的订单ID',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    warehouse_ids VARCHAR(255) COMMENT '货物所在的仓库ID列表 (例如 "1,3")',
    status VARCHAR(50) NOT NULL COMMENT '配送状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    INDEX idx_order_id (order_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配送信息表';