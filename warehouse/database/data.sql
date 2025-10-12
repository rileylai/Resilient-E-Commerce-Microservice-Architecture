-- Sample data for Warehouse Service
USE warehouse;

-- Insert sample warehouses
INSERT INTO warehouse (name, address, status) VALUES
('Central Warehouse', '123 Main St, Sydney NSW 2000', 'ACTIVE'),
('East Warehouse', '456 East Rd, Sydney NSW 2010', 'ACTIVE'),
('West Warehouse', '789 West Ave, Sydney NSW 2020', 'ACTIVE'),
('North Warehouse', '321 North Blvd, Sydney NSW 2030', 'INACTIVE');

-- Insert sample products
INSERT INTO product (name, description, price) VALUES
('Laptop', 'High-performance laptop with 16GB RAM', 1299.99),
('Mouse', 'Wireless optical mouse', 29.99),
('Keyboard', 'Mechanical keyboard with RGB lighting', 89.99),
('Monitor', '27-inch 4K UHD monitor', 499.99),
('Headphones', 'Noise-cancelling wireless headphones', 199.99),
('USB Cable', 'USB-C to USB-A cable 2m', 12.99),
('Webcam', '1080p HD webcam', 79.99),
('Desk Lamp', 'LED desk lamp with adjustable brightness', 45.99);

-- Insert sample inventory
-- Central Warehouse
INSERT INTO inventory (warehouse_id, product_id, available_quantity, reserved_quantity) VALUES
(1, 1, 50, 5),   -- Laptop
(1, 2, 200, 10),  -- Mouse
(1, 3, 150, 8),   -- Keyboard
(1, 4, 80, 4),    -- Monitor
(1, 5, 100, 6);   -- Headphones

-- East Warehouse
INSERT INTO inventory (warehouse_id, product_id, available_quantity, reserved_quantity) VALUES
(2, 1, 30, 2),    -- Laptop
(2, 2, 180, 12),  -- Mouse
(2, 4, 60, 3),    -- Monitor
(2, 6, 500, 20),  -- USB Cable
(2, 7, 90, 5);    -- Webcam

-- West Warehouse
INSERT INTO inventory (warehouse_id, product_id, available_quantity, reserved_quantity) VALUES
(3, 3, 120, 6),   -- Keyboard
(3, 5, 75, 4),    -- Headphones
(3, 6, 450, 15),  -- USB Cable
(3, 7, 110, 7),   -- Webcam
(3, 8, 200, 10);  -- Desk Lamp

-- Insert sample stock reservations
INSERT INTO stock_reservation (order_id, warehouse_id, product_id, quantity, status) VALUES
('ORD-20251012-001', 1, 1, 2, 'RESERVED'),
('ORD-20251012-002', 1, 2, 5, 'CONFIRMED'),
('ORD-20251012-003', 2, 4, 1, 'RESERVED'),
('ORD-20251012-004', 3, 5, 3, 'RELEASED');
