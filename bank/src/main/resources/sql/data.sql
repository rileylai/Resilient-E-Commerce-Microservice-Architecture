-- Initial data for Bank microservice
-- This script inserts test accounts for customer and store

-- Insert customer account (user_id = 1)
-- Balance: 1,000 AUD for testing
INSERT INTO accounts (user_id, balance, currency, created_at)
VALUES ('1', 1000.0000, 'AUD', CURRENT_TIMESTAMP);

-- Insert store account (user_id = 2) - Reserved for receiving payments
-- Balance: 0 AUD (will receive payments from customers)
INSERT INTO accounts (user_id, balance, currency, created_at)
VALUES ('2', 0.0000, 'AUD', CURRENT_TIMESTAMP);

