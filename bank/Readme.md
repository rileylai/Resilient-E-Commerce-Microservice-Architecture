# Bank Microservice

Enterprise-grade Spring Boot service that safeguards wallet balances, executes debits/refunds, and keeps the marketplace ledger consistent. It exposes synchronous REST APIs, consumes RabbitMQ events, and persists every transaction in MySQL with strict ordering rules.

---

## Overview
- **Purpose**: Central payment hub that enforces one-wallet-per-currency, powers all debits/refunds, and publishes authoritative transaction outcomes.
- **Scope**: Handles customer wallet management, store settlement (store wallet is user `2`), balances RabbitMQ-driven retries, and guarantees each order is charged only once.

---

## Tech Stack
- **Spring Boot 3** (REST, dependency management)
- **Java 17**
- **MyBatis-Plus** (ORM / query helpers)
- **MySQL 8+**
- **RabbitMQ 3.x**
- **ModelMapper**, **Lombok**
- **Maven 3.9+**

---

## Key Features
1. **Order-based debit idempotency** – `orderId` is unique per successful debit, blocking duplicate charges.
2. **Double-entry fund transfers** – Debits move funds user → store (`userId = "2"`); refunds reverse the flow.
3. **Hybrid communications** – REST endpoints for synchronous calls, RabbitMQ pipeline for async retries.
4. **DTO isolation** – Clean separation between transport models and persistence entities.
5. **Consistent error model** – `Result<T>` responses with domain error codes, rich logging, and tracing.

---

## REST API Usage
Base URL: `http://localhost:8081`

### `POST /api/bank/debit`
- **Purpose**: Charge a customer wallet and credit the store wallet for a specific order.
- **Typical use**: Store service confirms checkout success.
- **Request**
```json
{
  "orderId": "order001",
  "userId": "1",
  "amount": 150.00,
  "currency": "AUD"
}
```
- **Success**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 9001,
    "orderId": "order001",
    "userId": "1",
    "txType": "DEBIT",
    "status": "SUCCEEDED",
    "amount": 150.00,
    "currency": "AUD"
  }
}
```
- **Duplicate order failure**
```json
{
  "code": 511,
  "message": "Order already debited",
  "data": null
}
```

---

### `POST /api/bank/refund`
- **Purpose**: Move funds back from the store wallet to the customer.
- **Typical use**: Order cancellation or partial refund.
- **Request**
```json
{
  "orderId": "order001",
  "userId": "1",
  "amount": 50.00,
  "currency": "AUD",
  "idempotencyKey": "order001-refund01"
}
```
- **Idempotency tip**: use a unique key per refund attempt (`order001-refund01`, `order001-refund02`, …). The service accumulates all successful refunds per order and rejects totals that exceed the original debit.
- **Success**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 9100,
    "orderId": "order001",
    "userId": "1",
    "txType": "REFUND",
    "status": "SUCCEEDED",
    "amount": 50.00,
    "currency": "AUD"
  }
}
```
- **Exceeds debit failure**
```json
{
  "code": 512,
  "message": "Refund exceeds available balance",
  "data": null
}
```

---

### `GET /api/account/{userId}/{currency}`
- **Purpose**: Fetch a wallet by user + currency.
- **Typical use**: Customer portal, store balance verification.
- **Success**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": "42",
    "currency": "AUD",
    "balance": 725.50,
    "createdAt": "2024-11-19T19:32:11"
  }
}
```
- **Missing account**
```json
{
  "code": 404,
  "message": "Account not found for user 42 and currency AUD",
  "data": null
}
```

---

## RabbitMQ Integration

- **Incoming queue**: `bank.transactions.request`
- **Outgoing queue**: `bank.transactions.result`

### Event payloads (`TransactionRequestEventDTO`)
**Debit event**
```json
{
  "eventType": "DEBIT_REQUEST",
  "orderId": "order001",
  "userId": "1",
  "amount": 150.00,
  "currency": "AUD",
  "timestamp": "2024-11-20T10:15:30Z"
}
```
**Refund event**
```json
{
  "eventType": "REFUND_REQUEST",
  "orderId": "order001",
  "userId": "1",
  "amount": 50.00,
  "currency": "AUD",
  "timestamp": "2024-11-21T08:02:03Z",
  "idempotencyKey": "order001-refund01"
}
```

### Processing lifecycle
1. **Consumer** receives the event and hands it to `TransactionRequestHandlerService`.
2. **Handler** normalizes the event type, checks for a previously successful transaction (`orderId + txType`), and skips duplicates.
3. **Service layer** executes `processDebit` or `processRefund` with the same validations as REST.
4. **Persistence** ensures a `transactions` row plus updated account balances.
5. **Publisher** emits a `TransactionResultEventDTO` back to `bank.transactions.result`.

### Result examples
**Success**
```json
{
  "orderId": "order001",
  "userId": "1",
  "txType": "DEBIT",
  "status": "SUCCEEDED",
  "amount": 150.00,
  "currency": "AUD",
  "message": "Debit succeeded"
}
```
**Failure**
```json
{
  "orderId": "order001",
  "userId": "1",
  "txType": "DEBIT",
  "status": "FAILED",
  "amount": 150.00,
  "currency": "AUD",
  "message": "Insufficient funds"
}
```

---

## Business Logic
### Debit
1. Reject if `orderId` already has a successful debit (idempotency by order).
2. Lock customer `(userId, currency)` and store `(userId = "2", currency)` accounts.
3. Confirm customer funds ≥ requested amount.
4. Subtract from customer, add to store, persist `DEBIT / SUCCEEDED`.

### Refund
1. Find the original successful debit and ensure cumulative refunds ≤ debit amount.
2. Lock customer and store accounts in the target currency.
3. Confirm store wallet has enough funds to return.
4. Subtract from store, add to customer, persist `REFUND / SUCCEEDED` keyed by `idempotencyKey` so retries for the same refund attempt (e.g., `order001-refund-01`, `order001-refund-02`) do not double-credit the customer.

---

## Project Structure
```
src/main/java/com/tut2/group3/bank
├── controller/   # REST endpoints
├── service/      # Interfaces + implementations
├── dto/          # Request/response objects
├── entity/       # MyBatis-Plus entities
├── repository/   # Mapper interfaces
├── consumer/     # RabbitMQ listeners
└── producer/     # RabbitMQ publishers
```

---

## Setup Instructions
1. **Clone** and `cd bank`.
2. **Configure DB & RabbitMQ** in `src/main/resources/application.properties`.
3. **Apply schema** using `src/main/resources/sql/schema.sql` against `5348_bank_service_db`.
4. **Build**: `mvn clean install`.
5. **Run**: `mvn spring-boot:run` (listens on port `8081`).
6. **Smoke test**: `curl http://localhost:8081/actuator/health`.

---

## Notes
- Store systems never need the store wallet ID; the Bank service hardcodes it as `"2"`.
- Each user may hold **only one account per currency**. Violations are rejected at the service and DB level.
- `orderId` must be unique per successful debit—reuse triggers `Order already debited`.

---

Questions or onboarding needs? Reach out to the platform team—happy shipping! 💸
