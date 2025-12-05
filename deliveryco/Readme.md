# DeliveryCo Microservice

Enterprise-grade Spring Boot service that orchestrates deliveries, tracks shipments, and emits status telemetry across the marketplace. It consumes Store fulfillment events through RabbitMQ topic exchanges, persists delivery records in MySQL, and asynchronously notifies Store and Email services as shipments progress or fail.

---

## Overview
- **Purpose**: Delivery control plane that receives fulfillment requests, schedules carrier actions, preserves shipment history, and publishes authoritative delivery outcomes for every order.
- **Scope**: Handles delivery intake from the Store microservice via `delivery.request.queue`, manages tracking checkpoints (picked up, in transit, delivered, lost), and escalates failures to Email services while keeping checkout flows decoupled through asynchronous messaging.

---

## Tech Stack
- **Spring Boot 3** (REST controllers, schedulers, AMQP support)
- **Java 17**
- **RabbitMQ 3.x** (topic exchanges, durable queues)
- **MyBatis-Plus** (MySQL 8+ persistence and query helpers)
- **Docker** (container packaging and reproducible deployments)
- **Lombok** (concise entities and DTOs)
- **Logback** (structured, leveled delivery telemetry)

---

## Key Responsibilities
1. **Intake delivery requests** – Consume fulfillment events from the Store microservice listening on `delivery.request.queue`.
2. **Track shipment lifecycle** – Transition events through picked up, in transit, delivered, or lost states with strict validation.
3. **Publish status updates** – Emit asynchronous confirmations back to Store via `delivery.status.queue` so downstream services stay in sync.
4. **Escalate failures** – Trigger `email.orderfail.queue` notifications whenever deliveries fail or are marked lost so customers are notified automatically.

---

## Architecture & Workflow
- **RabbitMQ listener**: `DeliveryRequestListener` subscribes to `delivery.request.queue`, uses manual acknowledgments, and pipelines payloads into `DeliveryService` for business orchestration while status updates are published asynchronously through the `mq` module.
- **Idempotent processing**: Each `DeliveryRequestDTO` is keyed by `orderId + deliveryId`; duplicate events hit the repository cache and are skipped so retries never create double shipments.
- **Durable queues & DTO contracts**: All queues are declared durable and messages travel as structured JSON DTOs (`DeliveryRequestDTO`, `DeliveryStatusUpdateDTO`) to keep Store, DeliveryCo, and Email schemas aligned.
- **Asynchronous collaboration**: Store and Email services communicate through RabbitMQ topic exchanges, allowing DeliveryCo to process spikes without blocking checkout or email workflows.
- **Horizontal scale**: Multiple DeliveryCo pods can consume from the same queues, with RabbitMQ distributing messages round-robin to sustain high throughput without cross-node coordination.

---

## REST API Usage
Base URL: `http://localhost:8083`

### `GET /api/delivery/{deliveryId}/status`
- **Purpose**: Provide admin or monitoring access to the live status of a delivery.
- **Typical use**: Operations dashboards or support tooling verifying shipment progress outside of asynchronous events.
- **Success**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "deliveryId": 7301,
    "orderId": "order001",
    "status": "IN_TRANSIT",
    "updatedAt": "2024-11-21T08:02:03Z"
  }
}
```
- **Not found**
```json
{
  "code": 404,
  "message": "Delivery not found with ID: 7301",
  "data": null
}
```

---

## RabbitMQ Integration

- **Incoming queue**: `delivery.request.queue`
- **Outgoing queue**: `delivery.status.queue`
- **Failure queue**: `email.orderfail.queue`

### Event payloads (`DeliveryRequestDTO`, `DeliveryStatusUpdateDTO`)
**Delivery request**
```json
{
  "eventType": "DELIVERY_REQUEST",
  "orderId": "order001",
  "deliveryId": 7301,
  "customerId": 42,
  "address": "1 Market St, Sydney NSW",
  "items": [
    {
      "sku": "SKU-1001",
      "quantity": 1
    }
  ],
  "timestamp": "2024-11-20T10:15:30Z"
}
```
**Status update**
```json
{
  "eventType": "DELIVERY_STATUS",
  "deliveryId": 7301,
  "orderId": "order001",
  "status": "IN_TRANSIT",
  "statusDetail": "Departed sorting center",
  "timestamp": "2024-11-21T08:02:03Z"
}
```

### Processing lifecycle
1. **Consumer** receives a message and verifies the dedup key (`orderId + deliveryId`).
2. **Service layer** validates payloads, persists or updates the delivery aggregate, and schedules hand-offs to carriers.
3. **Status publisher** emits `DeliveryStatusUpdateDTO` events to `delivery.status.queue`.
4. **Failure handler** routes lost deliveries or repeated timeouts to `email.orderfail.queue` to fan out email notifications.
5. **Acknowledgment** occurs only after DB + publish succeed, guaranteeing at-least-once delivery without duplication.

### Example message flow
```text
Store -> [delivery.request.queue] -> DeliveryCo -> [delivery.status.queue] -> Store -> EmailService
```

---

## Fault Tolerance & Reliability
- **Retry + DLQ**: Transient processing errors trigger bounded retries; stubborn messages land in a dedicated dead-letter queue for manual inspection without blocking the main consumer.
- **Compensating actions**: LOST deliveries publish to `email.orderfail.queue`, prompting customer notifications and allowing Store to resubmit or refund.
- **Durable messaging**: RabbitMQ queues are durable and messages are persisted with delivery mode 2 so requests and status updates survive broker restarts.
- **Transactional safety**: Delivery status updates are wrapped in MyBatis-Plus transactions; events are published only after the database commit succeeds, ensuring downstream consumers never see phantom states.

---

## Project Structure
```
src/main/java/com/tut2/group3/deliveryco
├── DeliverycoApplication.java   # Spring Boot bootstrap
├── controller/                 # REST endpoints (admin/monitoring)
├── service/                    # Interfaces + implementations
├── dto/                        # Request/response & event DTOs
├── entity/                     # MyBatis-Plus entities
├── repository/                 # Mapper interfaces
├── listener/                   # RabbitMQ consumers
├── mq/                         # Publishers and message helpers
├── scheduler/                  # Status progression + retry jobs
├── config/                     # RabbitMQ, security, and app config
├── common/                     # Error codes, constants
├── interceptor/                # JWT/auth interceptors
└── util/                       # Shared utilities (ThreadLocal, JWT)
```

---

## Setup Instructions
1. **Clone** the repo and `cd deliveryco`.
2. **Configure DB & RabbitMQ** credentials in `src/main/resources/application.properties`.
3. **Apply schema** using `src/main/resources/sql/schema.sql` against the `delivery_service` database.
4. **Build**: `mvn clean install`.
5. **Run**: `mvn spring-boot:run` (exposes REST + RabbitMQ workers on port `8083`).
6. **Smoke test**: `curl http://localhost:8083/api/delivery/health`.

---

## Notes
- Delivery statuses advance PENDING -> PICKED_UP -> IN_TRANSIT -> DELIVERED/LOST; replays respect final states to avoid double updates.
- `delivery.package-loss-rate` controls simulated loss percentages for testing compensating actions.
- Admin APIs inherit JWT validation, so monitoring clients must supply valid tokens.
