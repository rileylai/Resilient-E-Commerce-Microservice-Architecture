# Store Microservice

Enterprise-grade Spring Boot service that orchestrates storefront experiences, authenticates users, processes orders end-to-end, and coordinates downstream microservices via REST and RabbitMQ. It acts as the primary gateway for web/mobile clients and maintains transactional consistency across Warehouse, Bank, DeliveryCo, and EmailService integrations.

---

## Overview
- **Purpose**: Central orchestration layer that owns checkout logic, order lifecycle management, and user identity while shielding clients from the complexity of distributed workflows.
- **Scope**: Manages user authentication (JWT), order placement, stock verification, payment execution, delivery coordination, and notification fan-out. Combines synchronous REST calls and asynchronous RabbitMQ messaging so the frontend experiences low latency while backend services scale independently.

---

## Tech Stack
- **Spring Boot 3** (REST controllers, Feign clients, scheduling)
- **Java 17**
- **MyBatis-Plus** (order + user persistence)
- **RabbitMQ 3.x** (delivery + email event mesh)
- **JWT / Spring Security** (token-based auth)
- **REST APIs + OpenFeign** (Bank/Warehouse integration)
- **Docker** (container packaging)
- **Lombok**, **Logback**

---

## Key Responsibilities
1. **Identity & access** – Register users, validate credentials, and issue JWTs that gate protected endpoints.
2. **Inventory gates** – Call the Warehouse microservice to validate availability and reserve stock before any payment attempt.
3. **Payment orchestration** – Execute debit/refund flows through the Bank microservice’s REST APIs with strict idempotency controls.
4. **Delivery coordination** – Publish `delivery.request.queue` messages and listen to `delivery.status.queue` updates to keep orders aligned with DeliveryCo progress.
5. **Notification fan-out** – Emit `email.orderfail.queue` + `email.refund.queue` events so EmailService informs customers of success, failure, or refunds.

---

## Architecture & Workflow
- **Hybrid communications**: REST (synchronous) to Warehouse/Bank for stock checks and payments that require immediate responses; RabbitMQ (asynchronous) to DeliveryCo and EmailService for long-running logistics and customer messaging.
- **Layered design**:
  - **Controller layer** (`controller/`) – Exposes REST endpoints for auth, catalog, and order flows.
  - **Service layer** (`service/`, `serviceImpl/`) – Encapsulates orchestration logic, retries, and compensating actions.
  - **Mapper layer** (`mapper/`) – MyBatis-Plus mappers for `users`, `orders`, and `order_item` tables.
  - **Entity/DTO layer** (`pojo/`, `dto/`) – Separates persistence entities from transport DTOs for clean API boundaries.
- **Cross-service clients**: OpenFeign clients (`client/`) call Bank and Warehouse; `MessagePublisher` handles delivery/email messages while `DeliveryStatusListener` ingests updates on `delivery.status.queue`.
- **Scalability**: Stateless controllers + idempotent service methods allow horizontal scaling behind the API gateway; durable queues decouple spikes in demand.

---

## REST API Usage
Base URL: `http://localhost:8080`

> Note: When fronted by the API gateway, `/user/**` and `/order/**` endpoints are exposed as `/api/auth/**` and `/api/order/**`.

### `POST /api/auth/login`
- **Purpose**: Authenticate users (proxy to `/user/login`) and issue JWT tokens for subsequent requests.
- **Typical use**: Frontend login form exchanging credentials for a short-lived bearer token.
- **Request**
```json
{
  "username": "alice",
  "password": "P@ssword123"
}
```
- **Success**
```json
{
  "code": 200,
  "message": "success",
  "data": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### `POST /api/order/create`
- **Purpose**: Place a new order (proxy to `/order/place`) and trigger the full orchestration pipeline.
- **Typical use**: Checkout flow submitting user/cart payloads.
- **Request**
```json
{
  "userId": 1,
  "items": [
    { "productId": 101, "quantity": 1, "price": 750.00 }
  ],
  "currency": "AUD",
  "shippingAddress": "1 Market St, Sydney NSW"
}
```
- **Success**
```json
{
  "code": 200,
  "message": "Order placed",
  "data": {
    "orderId": 5001,
    "status": "PAYMENT_SUCCESSFUL",
    "reservationId": "RSV-abc123"
  }
}
```

---

### `POST /api/order/cancel/{id}`
- **Purpose**: Cancel an in-flight order (proxy to `/order/cancel/{orderId}`), release stock, and trigger refunds if needed.
- **Typical use**: User-initiated cancellation prior to carrier pickup.
- **Success**
```json
{
  "code": 200,
  "message": "Order cancelled and refund initiated",
  "data": null
}
```

---

### `GET /api/order/{id}`
- **Purpose**: Retrieve live order status, including payment and delivery milestones.
- **Typical use**: Account order history, CS dashboards, or polling UI widgets.
- **Success**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderId": 5001,
    "status": "DELIVERED",
    "deliveryTracking": "In transit -> Delivered",
    "totalAmount": 750.00
  }
}
```

---

## RabbitMQ Integration

- **Outgoing queues**: `delivery.request.queue`, `delivery.cancellation.queue`, `email.orderfail.queue`, `email.refund.queue`
- **Incoming queue**: `delivery.status.queue`
- **Exchanges**: `delivery.exchange` (DeliveryCo), `store.exchange` (EmailService notifications)

### Processing lifecycle
1. **Order placement** publishes `DeliveryRequestDto` to `delivery.request.queue` once payment clears.
2. **Delivery status updates** arrive on `delivery.status.queue` and drive order state transitions (e.g., `IN_TRANSIT`, `DELIVERED`, `LOST`).
3. **Compensations** publish cancellations/refunds to the relevant queues whenever orders fail or customers cancel.

---

## Fault Tolerance & Reliability
- **Compensating actions**: On downstream failures, the service releases reserved stock, issues bank refunds, and pushes failure emails so users are never double-charged.
- **Durable messaging**: All delivery/email queues are durable with JSON payloads, guaranteeing persistence during broker restarts.
- **Idempotent refunds**: Refund requests carry deterministic keys (`orderId + reason`) to prevent duplicate credits during retries.
- **Timeout + rollback**: Feign/Rabbit operations use timeouts; stalled workflows automatically transition to FAILED/CANCELLED states to keep consistency and deliver >99.9% uptime targets.

---

## Example Workflow
```text
User -> Store -> Warehouse (stock check) -> Bank (payment)
     -> RabbitMQ (delivery.request.queue) -> DeliveryCo
     -> RabbitMQ (delivery.status.queue) -> Store
     -> RabbitMQ (email.*.queue) -> EmailService
```
This pipeline keeps latency-sensitive tasks synchronous while large-scale logistics and customer comms run asynchronously for resilience and scale.

---

## Database Design
- **`users`** – Stores username, password hash, and email for JWT issuance.
- **`orders`** – Tracks order lifecycle, reservation IDs, Bank transaction IDs, and timestamps.
- **`order_item`** – Line-item breakdown per order, including product metadata and pricing.

All tables are managed via MyBatis-Plus mappers (`UserMapper`, `OrderMapper`, `OrderItemMapper`) and follow the Database-per-Service pattern for loose coupling.

---

## Project Structure
```
src/main/java/com/tut2/group3/store
├── controller/        # Auth + order REST APIs
├── service/           # Contracts (OrderService, UserService, MessagePublisher)
├── service/serviceImpl# Business orchestration + compensations
├── client/            # Feign clients for Bank & Warehouse
├── listener/          # RabbitMQ consumers (delivery status)
├── config/            # RabbitMQ, Feign, security config
├── mapper/            # MyBatis-Plus mappers
├── dto/               # Transport models (auth, order, delivery, email)
├── pojo/              # Persistence entities
├── interceptor/, util/# JWT + cross-cutting helpers
└── resources/         # `application.yml`, SQL schema/data seeds
```

---

## Setup Instructions
1. **Clone** and `cd store`.
2. **Configure** database, RabbitMQ, and JWT secrets in `src/main/resources/application.yml`.
3. **Apply schema/data** using `src/main/resources/sql/schema.sql` & `data.sql` against the `mobile_sales` database.
4. **Install deps**: `mvn clean install`.
5. **Run**: `mvn spring-boot:run` (listens on `8080`, Swagger at `/swagger-ui.html`).
6. **Smoke tests**: `curl http://localhost:8080/user/secure` (JWT required) and `curl http://localhost:8080/order/{id}`.

---

## Notes
- JWT tokens use secret `jD8nFz7eA9hQ2LmBt4KxVwR1zTYuE3gH` with 24-hour expiry; rotate via environment overrides for production.
- RabbitMQ credentials default to `admin/admin`; update via `SPRING_RABBITMQ_*` env vars before deploying.
- Swagger UI is enabled for rapid consumer onboarding (`http://localhost:8080/swagger-ui/index.html#/`).


