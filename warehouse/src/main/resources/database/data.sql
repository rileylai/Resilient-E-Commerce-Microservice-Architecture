-- Sample data for Warehouse Service

-- Insert sample warehouses (skip if already exists)
INSERT IGNORE INTO warehouse (id, name, address, status) VALUES
(1, 'Central Warehouse', '123 Main St, Sydney NSW 2000', 'ACTIVE'),
(2, 'East Warehouse', '456 East Rd, Sydney NSW 2010', 'ACTIVE'),
(3, 'West Warehouse', '789 West Ave, Sydney NSW 2020', 'ACTIVE'),
(4, 'North Warehouse', '321 North Blvd, Sydney NSW 2030', 'INACTIVE');

-- Insert sample products (skip if already exists)
INSERT IGNORE INTO product (id, name, description, price) VALUES
(1, 'Laptop', 'High-performance laptop with 16GB RAM', 1299.99),
(2, 'Mouse', 'Wireless optical mouse', 29.99),
(3, 'Keyboard', 'Mechanical keyboard with RGB lighting', 89.99),
(4, 'Monitor', '27-inch 4K UHD monitor', 499.99),
(5, 'Headphones', 'Noise-cancelling wireless headphones', 199.99),
(6, 'USB Cable', 'USB-C to USB-A cable 2m', 12.99),
(7, 'Webcam', '1080p HD webcam', 79.99),
(8, 'Desk Lamp', 'LED desk lamp with adjustable brightness', 45.99);

-- Insert sample inventory (skip if already exists)
-- Central Warehouse
INSERT IGNORE INTO inventory (warehouse_id, product_id, available_quantity, reserved_quantity) VALUES
(1, 1, 50, 5),   -- Laptop
(1, 2, 200, 10),  -- Mouse
(1, 3, 150, 8),   -- Keyboard
(1, 4, 80, 4),    -- Monitor
(1, 5, 100, 6);   -- Headphones

-- East Warehouse
INSERT IGNORE INTO inventory (warehouse_id, product_id, available_quantity, reserved_quantity) VALUES
(2, 1, 30, 2),    -- Laptop
(2, 2, 180, 12),  -- Mouse
(2, 4, 60, 3),    -- Monitor
(2, 6, 500, 20),  -- USB Cable
(2, 7, 90, 5);    -- Webcam

-- West Warehouse
INSERT IGNORE INTO inventory (warehouse_id, product_id, available_quantity, reserved_quantity) VALUES
(3, 3, 120, 6),   -- Keyboard
(3, 5, 75, 4),    -- Headphones
(3, 6, 450, 15),  -- USB Cable
(3, 7, 110, 7),   -- Webcam
(3, 8, 200, 10);  -- Desk Lamp

-- Insert sample stock reservations (skip if already exists)
INSERT IGNORE INTO stock_reservation (id, order_id, warehouse_id, product_id, quantity, status) VALUES
(1, 'ORD-20251012-001', 1, 1, 2, 'RESERVED'),
(2, 'ORD-20251012-002', 1, 2, 5, 'CONFIRMED'),
(3, 'ORD-20251012-003', 2, 4, 1, 'RESERVED'),
(4, 'ORD-20251012-004', 3, 5, 3, 'RELEASED');
