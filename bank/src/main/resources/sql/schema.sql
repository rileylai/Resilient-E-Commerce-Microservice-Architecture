-- Database schema for Bank microservice
CREATE DATABASE IF NOT EXISTS `5348_bank_service_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `5348_bank_service_db`;

-- Create accounts table for Bank microservice
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    currency VARCHAR(3) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_accounts_user_currency UNIQUE (user_id, currency),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0),
    INDEX idx_accounts_currency (currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create transactions table for Bank microservice
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    tx_type ENUM('DEBIT', 'REFUND') NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3),
    status ENUM('REQUESTED', 'SUCCEEDED', 'FAILED') NOT NULL,
    bank_tx_id VARCHAR(64) NOT NULL,
    message VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idempotency_key VARCHAR(128),
    CONSTRAINT uk_transactions_bank_tx_id UNIQUE (bank_tx_id),
    CONSTRAINT uk_transactions_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_transactions_amount_non_negative CHECK (amount >= 0),
    INDEX idx_transactions_order_id (order_id),
    INDEX idx_transactions_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
